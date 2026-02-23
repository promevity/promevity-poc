package com.promevity.medai.domain.model;

/**
 * Value Object — ein Eintrag in der Differenzialdiagnose-Liste.
 *
 * <p><b>Hexagonale Architektur — Domain Layer</b><br>
 * Jeder Eintrag repräsentiert eine Erkrankung mit ihrer posterioren
 * Wahrscheinlichkeit, berechnet vom {@code BayesianRiskService}.
 * Die Liste aller Einträge bildet das Differential Diagnosis Ranking,
 * das im {@link RiskAssessment} gehalten wird.
 *
 * @param diseaseName           Name der Erkrankung
 * @param probabilityPercentage posteriore Wahrscheinlichkeit in Prozent (0–100)
 */
public record DifferentialDiagnosis(
        String diseaseName,
        double probabilityPercentage
) {}
