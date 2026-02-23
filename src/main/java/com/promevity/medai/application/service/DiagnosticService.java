package com.promevity.medai.application.service;

import com.promevity.medai.application.port.in.DiagnoseResult;
import com.promevity.medai.application.port.in.DiagnoseUseCase;
import com.promevity.medai.application.port.in.GetPatientGraphUseCase;
import com.promevity.medai.application.port.in.IngestWearableDataUseCase;
import com.promevity.medai.application.port.in.PatientGraphResult;
import com.promevity.medai.application.port.in.RegisterPatientUseCase;
import com.promevity.medai.application.port.out.MedicalExplainerPort;
import com.promevity.medai.application.port.out.PatientRepositoryPort;
import com.promevity.medai.application.port.out.WearableRepositoryPort;
import com.promevity.medai.domain.model.Patient;
import com.promevity.medai.domain.model.RiskAssessment;
import com.promevity.medai.domain.model.Symptom;
import com.promevity.medai.domain.model.SymptomSource;
import com.promevity.medai.domain.model.VitalMeasurement;
import com.promevity.medai.domain.model.VitalReading;
import com.promevity.medai.domain.service.BayesianRiskService;
import com.promevity.medai.domain.service.VitalSignInterpreter;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application Service — Implementiert die primären Use-Case-Ports.
 *
 * <p><b>Hexagonale Architektur — Application Layer</b><br>
 * Diese Klasse ist der Orchestrator des Anwendungskerns.  Sie:
 * <ul>
 *   <li>implementiert {@link DiagnoseUseCase}, {@link RegisterPatientUseCase}
 *       und {@link IngestWearableDataUseCase} (primäre Ports),</li>
 *   <li>ruft {@link PatientRepositoryPort}, {@link WearableRepositoryPort}
 *       und {@link MedicalExplainerPort} (sekundäre Ports) auf,</li>
 *   <li>delegiert die reine Domänenlogik an {@link BayesianRiskService}
 *       und {@link VitalSignInterpreter}.</li>
 * </ul>
 *
 * <h2>Erweiterte Diagnose-Pipeline (8 Schritte)</h2>
 * <pre>
 *   1. NLP-Extraktion      — Rohtext → Symptome
 *   2. Vitaldaten-Fetch    — Garmin-Messwerte aus dem Digital Twin Graph
 *   3. Vital-Interpretation — Messwerte → Symptom-Signale (HR, HRV, SpO₂ …)
 *   4. Symptom-Merge       — NLP + Wearable dedupliziert zusammenführen
 *   5. Persistenz          — neue Symptome nach Neo4j schreiben
 *   6. GraphRAG            — vollständige Symptomhistorie abrufen
 *   7. Bayes-Inferenz      — Posteriori-Wahrscheinlichkeiten berechnen
 *   8. LLM-Erklärung      — inkl. Vitaldaten-Kontext
 * </pre>
 */
@ApplicationScoped
public class DiagnosticService implements DiagnoseUseCase, RegisterPatientUseCase, IngestWearableDataUseCase, GetPatientGraphUseCase {

    /** Zeitfenster für Vitaldaten-Abfrage (Demo: letzte 365 Tage, damit Seed-Daten greifen). */
    private static final int VITAL_LOOKBACK_HOURS = 365 * 24;

    @Inject PatientRepositoryPort  patientRepository;
    @Inject WearableRepositoryPort wearableRepository;
    @Inject BayesianRiskService    bayesianRiskService;
    @Inject VitalSignInterpreter   vitalSignInterpreter;
    @Inject MedicalExplainerPort   medicalExplainer;
    @Inject SymptomExtractor       symptomExtractor;

    // ─────────────────────────────────────────────────────────────────────────
    // DiagnoseUseCase
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public DiagnoseResult diagnose(String patientId, String rawText) {
        Log.infof("=== Use Case: diagnose START [patient=%s] ===", patientId);

        // Schritt 0 — Patient muss existieren
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException(
                        "Patient mit ID '%s' nicht gefunden".formatted(patientId)));

        // Schritt 1 — NLP-Extraktion
        List<Symptom> nlpSymptoms = symptomExtractor.extract(rawText);
        Log.infof("[1/8] NLP-Extraktion: %s", nlpSymptoms.stream().map(Symptom::name).toList());

        // Schritt 2 — Vitaldaten aus dem Digital Twin Graph laden
        List<VitalMeasurement> vitals = wearableRepository.findRecentByPatientId(
                patientId, VITAL_LOOKBACK_HOURS);
        Log.infof("[2/8] Wearable: %d Vitaldaten-Messungen geladen", vitals.size());

        // Schritt 3 — Vitaldaten → Symptom-Signale ableiten
        List<Symptom> vitalSymptoms = vitalSignInterpreter.interpret(vitals);
        Log.infof("[3/8] Vital-Interpretation: %s", vitalSymptoms.stream().map(Symptom::name).toList());

        // Schritt 4 — NLP + Wearable-Symptome dedupliziert zusammenführen
        List<Symptom> mergedSymptoms = merge(nlpSymptoms, vitalSymptoms);
        Log.infof("[4/8] Merge: %d Symptom(e) gesamt", mergedSymptoms.size());

        // Schritt 5 — Persistenz (idempotente MERGE-Operationen)
        for (Symptom s : mergedSymptoms) {
            patientRepository.saveSymptom(s);
            patientRepository.recordSymptom(patientId, s.id(), LocalDate.now());
        }
        Log.infof("[5/8] %d Symptom(e) im Knowledge Graph gespeichert", mergedSymptoms.size());

        // Schritt 6 — GraphRAG: vollständige Symptomhistorie abrufen
        List<Symptom> allSymptoms = patientRepository.findSymptomsByPatientId(patientId);
        Log.infof("[6/8] GraphRAG: %d kumulative Symptome geladen", allSymptoms.size());

        // Schritt 7 — Bayesianische Inferenz (Domain Service)
        RiskAssessment assessment = bayesianRiskService.assess(allSymptoms);
        Log.infof("[7/8] Bayes-Ergebnis: %s @ %.1f %%",
                assessment.diseaseName(), assessment.probabilityPercentage());

        // Schritt 8 — LLM-Erklärung inkl. Vitaldaten-Kontext
        String vitalSummary = vitalSignInterpreter.summarise(vitals);
        String explanation  = medicalExplainer.explain(patient.name(), assessment, rawText, vitalSummary);
        Log.infof("[8/8] LLM-Erklärung erzeugt (%d Zeichen)", explanation.length());

        // Symptom-Quellen berechnen: TEXT / GARMIN / TEXT+GARMIN / VERLAUF
        Set<String> nlpNames   = nlpSymptoms.stream().map(Symptom::name).collect(Collectors.toSet());
        Set<String> vitalNames = vitalSymptoms.stream().map(Symptom::name).collect(Collectors.toSet());
        List<SymptomSource> symptomSources = allSymptoms.stream()
                .map(s -> {
                    boolean fromNlp   = nlpNames.contains(s.name());
                    boolean fromVital = vitalNames.contains(s.name());
                    String src = (fromNlp && fromVital) ? "TEXT+GARMIN"
                               : fromVital              ? "GARMIN"
                               : fromNlp                ? "TEXT"
                               :                          "VERLAUF";
                    return new SymptomSource(s.name(), src);
                })
                .toList();

        // Strukturierte Vitaldaten für Frontend-Dashboard
        List<VitalReading> vitalReadings = vitalSignInterpreter.summariseStructured(vitals);

        List<String> symptomNames = allSymptoms.stream().map(Symptom::name).toList();

        Log.infof("=== Use Case: diagnose END [patient=%s] ===", patientId);
        return new DiagnoseResult(
                patientId,
                symptomNames,
                assessment.diseaseName(),
                assessment.probabilityPercentage(),
                assessment.differentialDiagnoses(),
                explanation,
                vitalSummary,
                symptomSources,
                vitalReadings
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IngestWearableDataUseCase
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void ingest(String patientId, List<VitalMeasurement> measurements) {
        patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException(
                        "Patient mit ID '%s' nicht gefunden".formatted(patientId)));
        wearableRepository.saveMeasurements(patientId, measurements);
        Log.infof("[Wearable] %d Messungen für Patient %s gespeichert",
                measurements.size(), patientId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RegisterPatientUseCase
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Patient register(Patient patient) {
        patientRepository.save(patient);
        Log.infof("Patient registriert [id=%s, name=%s]", patient.id(), patient.name());
        return patient;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GetPatientGraphUseCase
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public PatientGraphResult getPatientGraph(String patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException(
                        "Patient mit ID '%s' nicht gefunden".formatted(patientId)));
        List<PatientGraphResult.SymptomEntry> symptoms = patientRepository
                .findSymptomHistory(patientId)
                .stream()
                .map(h -> new PatientGraphResult.SymptomEntry(
                        h.symptom().id(), h.symptom().name(), h.latestDate()))
                .toList();
        Log.debugf("[Graph] Patient %s hat %d Symptom-Knoten im Digital Twin", patientId, symptoms.size());
        return new PatientGraphResult(patient.id(), patient.name(), patient.age(), symptoms);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hilfsmethoden
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Kombiniert zwei Symptom-Listen ohne Duplikate (Vergleich per Symptom-Name).
     * NLP-Symptome haben Vorrang; Wearable-Signale ergänzen, was fehlt.
     */
    private List<Symptom> merge(List<Symptom> primary, List<Symptom> secondary) {
        Map<String, Symptom> seen = new LinkedHashMap<>();
        primary.forEach(s -> seen.put(s.name(), s));
        secondary.forEach(s -> seen.putIfAbsent(s.name(), s));
        return new ArrayList<>(seen.values());
    }
}
