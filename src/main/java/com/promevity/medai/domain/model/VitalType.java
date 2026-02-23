package com.promevity.medai.domain.model;

/**
 * Enum der unterstützten Vitaldaten-Typen eines Wearables (z. B. Garmin).
 *
 * <p><b>Hexagonale Architektur — Domain Layer</b><br>
 * Reine Domänen-Enumeration ohne Framework-Abhängigkeiten.
 * Jeder Typ kennt seine Standard-Maßeinheit für Anzeige und Speicherung.
 */
public enum VitalType {

    /** Herzrate in Schlägen pro Minute. */
    HEART_RATE("bpm"),

    /** Herzratenvariabilität — Maß für die autonome Herzregulation. */
    HRV("ms"),

    /** Blutsauerstoffsättigung (Pulsoximetrie). */
    SPO2("%"),

    /** Garmin-Stresslevel auf einer Skala von 0–100. */
    STRESS_LEVEL("score"),

    /** Tagesschritte (Aktivitätsniveau). */
    STEPS_PER_DAY("steps"),

    /** Gesamtschlafdauer pro Nacht in Stunden. */
    SLEEP_DURATION_HOURS("h");

    private final String defaultUnit;

    VitalType(String defaultUnit) {
        this.defaultUnit = defaultUnit;
    }

    public String defaultUnit() {
        return defaultUnit;
    }
}
