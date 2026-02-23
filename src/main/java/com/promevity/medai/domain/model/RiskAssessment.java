package com.promevity.medai.domain.model;

import java.util.List;

/**
 * Value Object — Ergebnis des Bayesianischen Inferenzschritts.
 *
 * <p><b>Hexagonale Architektur — Domain Layer</b><br>
 * Dieses Objekt reist durch alle Schichten:
 * <ol>
 *   <li>Wird von {@code BayesianRiskService} (Domain) erzeugt.</li>
 *   <li>Wird von {@code DiagnosticService} (Application) konsumiert und
 *       an den {@code MedicalExplainerPort} weitergereicht.</li>
 *   <li>Wird im REST-Adapter in die {@code DiagnoseResponse} überführt.</li>
 * </ol>
 *
 * <p>Keine Framework-Annotationen — Serialisierung ist Sache des Adapters.
 *
 * @param diseaseName             Name der Erkrankung mit höchster posteriorer Wahrscheinlichkeit.
 * @param probabilityPercentage   Wahrscheinlichkeit in Prozent (0–100), 1 Dezimalstelle.
 * @param evidenceSymptoms        Symptom-Namen, die als Evidenz in das Netz eingeflossen sind.
 * @param differentialDiagnoses   Alle Erkrankungen absteigend nach Wahrscheinlichkeit sortiert.
 */
public record RiskAssessment(
        String diseaseName,
        double probabilityPercentage,
        List<String> evidenceSymptoms,
        List<DifferentialDiagnosis> differentialDiagnoses
) {
    public static RiskAssessment of(
            String diseaseName,
            double probabilityPercentage,
            List<String> evidenceSymptoms,
            List<DifferentialDiagnosis> differentialDiagnoses) {
        return new RiskAssessment(
                diseaseName,
                probabilityPercentage,
                List.copyOf(evidenceSymptoms),
                List.copyOf(differentialDiagnoses));
    }
}
