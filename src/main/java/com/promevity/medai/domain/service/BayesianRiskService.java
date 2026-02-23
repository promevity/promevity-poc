package com.promevity.medai.domain.service;

import com.promevity.medai.application.port.out.DiseaseKnowledgePort;
import com.promevity.medai.domain.model.DifferentialDiagnosis;
import com.promevity.medai.domain.model.DiseaseCptData;
import com.promevity.medai.domain.model.RiskAssessment;
import com.promevity.medai.domain.model.Symptom;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain Service — Graph-getriebener Bayesianischer Risiko-Kalkulator.
 *
 * <p><b>Hexagonale Architektur — Domain Layer</b><br>
 * Dieser Service kapselt die probabilistische Inferenz-Logik.
 * Die CPT-Parameter (Priors und Likelihoods) werden zur Laufzeit aus dem
 * Neo4j-Knowledge-Graph bezogen — über den sekundären Port
 * {@link DiseaseKnowledgePort}.  Dadurch ist die Wissensbasis ohne
 * Code-Änderungen erweiterbar (neue Krankheiten, angepasste Likelihoods).
 *
 * <h2>Naïve-Bayes-Formel (pro Erkrankung)</h2>
 * <pre>
 *   P(D|E) = [P(D) × ∏ P(eᵢ|D)] / [P(D) × ∏ P(eᵢ|D)  +  P(¬D) × ∏ P(eᵢ|¬D)]
 *
 *   Annahme: bedingte Unabhängigkeit der Symptome gegeben D.
 *   Nur beobachtete Symptome fließen ein; fehlendes Symptom liefert
 *   keine Gegenevidence (offene-Welt-Annahme).
 * </pre>
 *
 * <h2>Multi-Disease Ranking (Differential Diagnosis)</h2>
 * <pre>
 *   Für jede Erkrankung Di wird P(Di|E) unabhängig berechnet.
 *   Das Ergebnis ist ein absteigend sortiertes Ranking aller Erkrankungen
 *   (Differenzialdiagnose-Liste) plus der Top-1-Erkrankung als Hauptbefund.
 * </pre>
 *
 * <h2>Kalibrierte CPT-Werte (aus Neo4j, PoC-Daten)</h2>
 * <pre>
 * ┌────────────────────┬────────────────────────────────────┬──────────┐
 * │  Evidenz           │  Top-Erkrankung                    │  P(D|E)  │
 * ├────────────────────┬────────────────────────────────────┼──────────┤
 * │  {Tachy, Fatigue}  │  Thyroid Dysfunction               │  ≈ 54 %  │
 * │  {Tachy}           │  Thyroid Dysfunction               │  ≈ 40 %  │
 * │  {Fatigue}         │  Thyroid Dysfunction / Anemia      │  ≈ 24 %  │
 * │  {}  (keine)       │  Thyroid Dysfunction (Prior-Rang)  │  ≈ 15 %  │
 * └────────────────────┴────────────────────────────────────┴──────────┘
 * </pre>
 */
@ApplicationScoped
public class BayesianRiskService {

    @Inject
    DiseaseKnowledgePort diseaseKnowledgePort;

    /**
     * Berechnet für jede bekannte Erkrankung die posteriore Wahrscheinlichkeit
     * und gibt ein nach Wahrscheinlichkeit sortiertes {@link RiskAssessment}
     * zurück.
     *
     * @param symptoms vollständige Symptomliste aus dem Knowledge Graph
     * @return {@link RiskAssessment} mit Top-1-Erkrankung und Differenzialdiagnose-Ranking
     */
    public RiskAssessment assess(List<Symptom> symptoms) {
        Set<String> observed = symptoms.stream()
                .map(Symptom::name)
                .collect(Collectors.toSet());

        Log.debugf("[BayesNet] Eingabe-Evidenz: %s", observed);

        List<DiseaseCptData> diseases = diseaseKnowledgePort.loadAll();

        if (diseases.isEmpty()) {
            Log.warn("[BayesNet] Keine CPT-Daten verfügbar — gebe leeres Assessment zurück");
            return RiskAssessment.of("Unknown", 0.0, List.of(), List.of());
        }

        // Posteriori für jede Erkrankung berechnen und absteigend sortieren
        List<DifferentialDiagnosis> differentials = diseases.stream()
                .map(d -> {
                    double posterior = computePosterior(d, observed);
                    double pct = Math.round(posterior * 1000.0) / 10.0;
                    return new DifferentialDiagnosis(d.diseaseName(), pct);
                })
                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::probabilityPercentage).reversed())
                .toList();

        DifferentialDiagnosis top = differentials.get(0);
        List<String> evidence = List.copyOf(observed);

        Log.infof("[BayesNet] Top-Erkrankung: %s → %.1f %%  |  Differentials: %s",
                top.diseaseName(), top.probabilityPercentage(),
                differentials.stream()
                        .map(dd -> dd.diseaseName() + "=" + dd.probabilityPercentage() + "%")
                        .collect(Collectors.joining(", ")));

        return RiskAssessment.of(top.diseaseName(), top.probabilityPercentage(), evidence, differentials);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Naïve-Bayes-Posteriori für eine einzelne Erkrankung.
     *
     * <p>Nur Symptome, die sowohl beobachtet als auch in der CPT der Erkrankung
     * vorhanden sind, fließen in die Likelihood ein.  Unbekannte Symptome werden
     * ignoriert (offene-Welt-Annahme).
     */
    private double computePosterior(DiseaseCptData disease, Set<String> observed) {
        if (observed.isEmpty()) {
            return disease.prior();
        }

        double prior = disease.prior();
        double lD  = 1.0;
        double lND = 1.0;

        for (DiseaseCptData.SymptomLikelihood sl : disease.symptomLikelihoods()) {
            if (observed.contains(sl.symptomName())) {
                lD  *= sl.pGivenDisease();
                lND *= sl.pGivenNoDisease();
            }
        }

        // Keine bekannten Symptome der Erkrankung beobachtet → Prior zurückgeben
        if (lD == 1.0 && lND == 1.0) {
            return prior;
        }

        double numerator   = prior * lD;
        double denominator = numerator + (1.0 - prior) * lND;

        if (denominator == 0.0) {
            Log.warnf("[BayesNet] Denominator = 0 für '%s' — Fallback auf Prior", disease.diseaseName());
            return prior;
        }
        return numerator / denominator;
    }
}
