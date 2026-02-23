package com.promevity.medai.application.service;

import com.promevity.medai.application.port.in.DiagnoseResult;
import com.promevity.medai.application.port.in.DiagnoseUseCase;
import com.promevity.medai.application.port.in.RegisterPatientUseCase;
import com.promevity.medai.application.port.out.MedicalExplainerPort;
import com.promevity.medai.application.port.out.PatientRepositoryPort;
import com.promevity.medai.domain.model.Patient;
import com.promevity.medai.domain.model.RiskAssessment;
import com.promevity.medai.domain.model.Symptom;
import com.promevity.medai.domain.service.BayesianRiskService;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application Service — Implementiert die primären Use-Case-Ports.
 *
 * <p><b>Hexagonale Architektur — Application Layer</b><br>
 * Diese Klasse ist der Orchestrator des Anwendungskerns.  Sie:
 * <ul>
 *   <li>implementiert {@link DiagnoseUseCase} und {@link RegisterPatientUseCase}
 *       (primäre Ports — wird vom REST-Adapter aufgerufen),</li>
 *   <li>ruft {@link PatientRepositoryPort} und {@link MedicalExplainerPort}
 *       (sekundäre Ports — ruft Adapter/Out auf),</li>
 *   <li>delegiert die reine Domänenlogik an {@link BayesianRiskService}.</li>
 * </ul>
 *
 * <h2>Abhängigkeitsregel</h2>
 * <pre>
 *   REST-Adapter  →  DiagnosticService  →  BayesianRiskService (Domain)
 *                                       →  PatientRepositoryPort → Neo4j-Adapter
 *                                       →  MedicalExplainerPort → LLM-Adapter
 * </pre>
 * Pfeile zeigen immer vom Äußeren zum Inneren — niemals umgekehrt.
 */
@ApplicationScoped
public class DiagnosticService implements DiagnoseUseCase, RegisterPatientUseCase {

    @Inject PatientRepositoryPort patientRepository;
    @Inject BayesianRiskService   bayesianRiskService;
    @Inject MedicalExplainerPort  medicalExplainer;
    @Inject SymptomExtractor      symptomExtractor;

    // ─────────────────────────────────────────────────────────────────────────
    // DiagnoseUseCase
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Führt den 6-stufigen Diagnose-Workflow aus.
     *
     * <ol>
     *   <li><b>Extraktion</b>   — Rohtext → Symptom-Liste (NLP)</li>
     *   <li><b>Persistenz</b>   — Symptom-Knoten + Kanten nach Neo4j</li>
     *   <li><b>GraphRAG</b>     — vollständige Symptomhistorie aus dem Graph</li>
     *   <li><b>Inferenz</b>     — Bayesianische Posteriori-Berechnung</li>
     *   <li><b>Erklärung</b>    — LLM generiert patientengerechten Text</li>
     *   <li><b>Rückgabe</b>     — {@link DiagnoseResult} an den Adapter</li>
     * </ol>
     */
    @Override
    public DiagnoseResult diagnose(String patientId, String rawText) {
        Log.infof("=== Use Case: diagnose START [patient=%s] ===", patientId);

        // Schritt 0 — Patient muss existieren
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException(
                        "Patient mit ID '%s' nicht gefunden".formatted(patientId)));

        // Schritt 1 — NLP-Extraktion
        List<Symptom> extracted = symptomExtractor.extract(rawText);
        Log.infof("[1/5] Extrahiert: %s", extracted.stream().map(Symptom::name).toList());

        // Schritt 2 — Persistenz (idempotente MERGE-Operationen)
        for (Symptom s : extracted) {
            patientRepository.saveSymptom(s);
            patientRepository.recordSymptom(patientId, s.id(), LocalDate.now());
        }
        Log.infof("[2/5] %d Symptom(e) im Knowledge Graph gespeichert", extracted.size());

        // Schritt 3 — GraphRAG: vollständige Symptomhistorie abrufen
        List<Symptom> allSymptoms = patientRepository.findSymptomsByPatientId(patientId);
        Log.infof("[3/5] GraphRAG: %d kumulative Symptome geladen", allSymptoms.size());

        // Schritt 4 — Bayesianische Inferenz (Domain Service)
        RiskAssessment assessment = bayesianRiskService.assess(allSymptoms);
        Log.infof("[4/5] Bayes-Ergebnis: %s @ %.1f %%",
                assessment.diseaseName(), assessment.probabilityPercentage());

        // Schritt 5 — LLM-Erklärung (über sekundären Port)
        String explanation = medicalExplainer.explain(patient.name(), assessment, rawText);
        Log.infof("[5/5] LLM-Erklärung erzeugt (%d Zeichen)", explanation.length());

        List<String> symptomNames = allSymptoms.stream().map(Symptom::name).toList();

        Log.infof("=== Use Case: diagnose END [patient=%s] ===", patientId);
        return new DiagnoseResult(
                patientId,
                symptomNames,
                assessment.diseaseName(),
                assessment.probabilityPercentage(),
                assessment.differentialDiagnoses(),
                explanation
        );
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
}
