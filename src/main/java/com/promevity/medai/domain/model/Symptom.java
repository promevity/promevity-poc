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
 * <p>Die Konstanten werden sowohl vom Bayesianischen Risikodienst als auch
 * vom Symptom-Extraktor referenziert, ohne dass beide voneinander abhängig
 * sein müssen.
 */
public record Symptom(String id, String name) {

    /** Kanonischer Name für das Symptom "Tachykardie". */
    public static final String TACHYKARDIA     = "Tachykardia";

    /** Kanonischer Name für das Symptom "Müdigkeit / Erschöpfung". */
    public static final String FATIGUE          = "Fatigue";

    /** Kanonischer Name für das Symptom "Zittern". */
    public static final String TREMOR           = "Tremor";

    /** Kanonischer Name für das Symptom "Gewichtsverlust". */
    public static final String WEIGHT_LOSS      = "Weight Loss";

    /** Kanonischer Name für das Symptom "Wärmeunverträglichkeit". */
    public static final String HEAT_INTOLERANCE = "Heat Intolerance";

    /** Kanonischer Name für das Symptom "Angst / Unruhe". */
    public static final String ANXIETY          = "Anxiety";

    /** Kanonischer Name für das Symptom "Blässe". */
    public static final String PALLOR           = "Pallor";

    /** Kanonischer Name für das Symptom "Kurzatmigkeit". */
    public static final String BREATHLESSNESS   = "Breathlessness";

    /** Kanonischer Name für das Symptom "Muskelschwäche". */
    public static final String WEAKNESS         = "Weakness";

    /** Kanonischer Name für das Symptom "Brustschmerz". */
    public static final String CHEST_PAIN       = "Chest Pain";
}
