package com.promevity.medai.domain.model;

import java.util.List;

/**
 * Value Object — Conditional Probability Table (CPT) einer Erkrankung.
 *
 * <p><b>Hexagonale Architektur — Domain Layer</b><br>
 * Kapselt die aus dem Knowledge Graph geladenen Wahrscheinlichkeitsdaten
 * einer Erkrankung.  Wird von {@code BayesianRiskService} für die
 * Naïve-Bayes-Inferenz verwendet.
 *
 * <p>Das innere Record {@link SymptomLikelihood} trägt für jedes Symptom das
 * Likelihood-Paar P(s|D) und P(s|¬D), das direkt den
 * {@code HAS_SYMPTOM_LIKELIHOOD}-Kanten im Neo4j-Graphen entspricht.
 *
 * @param diseaseName         kanonischer Krankheitsname (übereinstimmend mit Neo4j-Node)
 * @param prior               Prior-Wahrscheinlichkeit P(D) in [0,1]
 * @param symptomLikelihoods  Liste der Symptom-Likelihood-Paare für diese Erkrankung
 */
public record DiseaseCptData(
        String diseaseName,
        double prior,
        List<SymptomLikelihood> symptomLikelihoods
) {
    /**
     * CPT-Eintrag für ein einzelnes Symptom.
     *
     * @param symptomName      kanonischer Symptomname (übereinstimmend mit Symptom-Knoten)
     * @param pGivenDisease    P(symptom | disease)
     * @param pGivenNoDisease  P(symptom | ¬disease)
     */
    public record SymptomLikelihood(
            String symptomName,
            double pGivenDisease,
            double pGivenNoDisease
    ) {}
}
