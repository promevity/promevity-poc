package com.promevity.medai.domain.model;

/**
 * Value Object — ein medizinisches Symptom aus der Ontologie-Schicht.
 *
 * <p><b>Hexagonale Architektur — Domain Layer</b><br>
 * Symptome bilden das kontrollierte Vokabular der Medical Ontology.
 * In Produktion würden sie mit SNOMED-CT- oder ICD-11-Konzept-IDs
 * angereichert; für den PoC reicht name-basiertes Matching.
 *
 * <p>Neo4j-Node-Label: {@code Symptom}<br>
 * Properties: {@code id}, {@code name}
 *
 * <p>Die Konstanten {@link #TACHYKARDIA} und {@link #FATIGUE} werden sowohl
 * vom Bayesianischen Risikodienst als auch vom Symptom-Extraktor referenziert,
 * ohne dass beide voneinander abhängig sein müssen.
 */
public record Symptom(String id, String name) {

    /** Kanonischer Name für das Symptom "Tachykardie". */
    public static final String TACHYKARDIA = "Tachykardia";

    /** Kanonischer Name für das Symptom "Müdigkeit / Erschöpfung". */
    public static final String FATIGUE = "Fatigue";
}
