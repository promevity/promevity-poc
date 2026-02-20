package com.promevity.medai.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Immutable value object that captures the output of the Bayesian risk engine.
 *
 * <p>Kept in the {@code service} package because it is produced and consumed
 * exclusively by the service layer; it is also serialised into the LLM prompt
 * (via {@link MedicalAssistant}).
 *
 * @param diseaseName           The disease with the highest posterior probability.
 * @param probabilityPercentage Probability expressed as a percentage (0–100).
 * @param evidenceSymptoms      The symptom names that were fed into the Bayesian
 *                              network as evidence — included for LLM explainability.
 */
public record RiskAssessment(

        @JsonProperty("diseaseName")
        String diseaseName,

        @JsonProperty("probabilityPercentage")
        double probabilityPercentage,

        @JsonProperty("evidenceSymptoms")
        List<String> evidenceSymptoms

) {
    /**
     * Convenience factory: constructs a {@code RiskAssessment} from a plain
     * symptom-name list (the common case used by the Bayesian engine).
     */
    public static RiskAssessment of(
            String diseaseName,
            double probabilityPercentage,
            List<String> evidenceSymptoms) {
        return new RiskAssessment(diseaseName, probabilityPercentage, evidenceSymptoms);
    }
}
