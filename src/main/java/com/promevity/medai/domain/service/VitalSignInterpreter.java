package com.promevity.medai.domain.service;

import com.promevity.medai.domain.model.Symptom;
import com.promevity.medai.domain.model.VitalMeasurement;
import com.promevity.medai.domain.model.VitalReading;
import com.promevity.medai.domain.model.VitalType;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Domain Service — interpretiert Wearable-Vitaldaten und leitet daraus
 * klinisch relevante Symptom-Signale ab.
 *
 * <p><b>Hexagonale Architektur — Domain Layer</b><br>
 * Reine Domänenlogik, keine Framework-Abhängigkeiten (außer CDI-Scope).
 * Konvertiert Durchschnittswerte der letzten 24 Stunden anhand evidenzbasierter
 * Schwellenwerte in {@link Symptom}-Objekte, die anschließend in die
 * Bayes'sche Inferenz einfließen.
 *
 * <h2>Schwellenwerte (PoC-kalibriert)</h2>
 * <pre>
 *   Herzrate  > 100 bpm  → Tachykardia
 *   HRV       <  30 ms   → Tachykardia (niedriger HRV = Herzrhythmusstörung)
 *   SpO₂      <  94 %    → Breathlessness
 *   Stress    >  70/100  → Anxiety
 *   Schlaf    <   6 h    → Fatigue
 * </pre>
 */
@ApplicationScoped
public class VitalSignInterpreter {

    // ── Klinische Schwellenwerte ──────────────────────────────────────────────
    private static final double HR_TACHYKARDIA_THRESHOLD      = 100.0;
    private static final double HRV_LOW_THRESHOLD             =  30.0;
    private static final double SPO2_BREATHLESSNESS_THRESHOLD =  94.0;
    private static final double STRESS_ANXIETY_THRESHOLD      =  70.0;
    private static final double SLEEP_FATIGUE_THRESHOLD       =   6.0;

    /**
     * Leitet aus Vitaldaten klinisch relevante Symptome ab.
     *
     * <p>Berechnet zunächst Durchschnittswerte pro {@link VitalType} und prüft
     * diese gegen die definierten Schwellenwerte.
     *
     * @param measurements Vitaldaten-Messwerte (können leer sein)
     * @return deduplizierte Liste abgeleiteter {@link Symptom}-Objekte
     */
    public List<Symptom> interpret(List<VitalMeasurement> measurements) {
        if (measurements.isEmpty()) {
            return List.of();
        }

        Map<VitalType, Double> averages = averageByType(measurements);
        Set<String> detectedNames = new LinkedHashSet<>();

        averages.forEach((type, avg) -> {
            switch (type) {
                case HEART_RATE -> {
                    if (avg > HR_TACHYKARDIA_THRESHOLD) {
                        detectedNames.add(Symptom.TACHYKARDIA);
                        Log.infof("[Vitals] Herzrate ⌀ %.0f bpm > %.0f → %s",
                                avg, HR_TACHYKARDIA_THRESHOLD, Symptom.TACHYKARDIA);
                    }
                }
                case HRV -> {
                    if (avg < HRV_LOW_THRESHOLD) {
                        detectedNames.add(Symptom.TACHYKARDIA);
                        Log.infof("[Vitals] HRV ⌀ %.0f ms < %.0f ms → %s (Herzrhythmus-Signal)",
                                avg, HRV_LOW_THRESHOLD, Symptom.TACHYKARDIA);
                    }
                }
                case SPO2 -> {
                    if (avg < SPO2_BREATHLESSNESS_THRESHOLD) {
                        detectedNames.add(Symptom.BREATHLESSNESS);
                        Log.infof("[Vitals] SpO₂ ⌀ %.1f %% < %.0f %% → %s",
                                avg, SPO2_BREATHLESSNESS_THRESHOLD, Symptom.BREATHLESSNESS);
                    }
                }
                case STRESS_LEVEL -> {
                    if (avg > STRESS_ANXIETY_THRESHOLD) {
                        detectedNames.add(Symptom.ANXIETY);
                        Log.infof("[Vitals] Stress ⌀ %.0f > %.0f → %s",
                                avg, STRESS_ANXIETY_THRESHOLD, Symptom.ANXIETY);
                    }
                }
                case SLEEP_DURATION_HOURS -> {
                    if (avg < SLEEP_FATIGUE_THRESHOLD) {
                        detectedNames.add(Symptom.FATIGUE);
                        Log.infof("[Vitals] Schlaf ⌀ %.1f h < %.0f h → %s",
                                avg, SLEEP_FATIGUE_THRESHOLD, Symptom.FATIGUE);
                    }
                }
                default -> { /* STEPS_PER_DAY — kein direktes Symptom-Mapping im PoC */ }
            }
        });

        return detectedNames.stream()
                .map(name -> new Symptom(
                        "symptom-" + name.toLowerCase(Locale.ROOT).replace(" ", "-"), name))
                .toList();
    }

    /**
     * Erstellt eine kompakte Vitaldaten-Zusammenfassung für den LLM-Prompt.
     *
     * <p>Beispielausgabe:
     * {@code Garmin-Vitaldaten (letzte 24 h): Herzrate ⌀ 108 bpm [↑], HRV ⌀ 24 ms [↓], ...}
     *
     * @param measurements Rohdaten
     * @return lesbarer Zusammenfassungsstring
     */
    public String summarise(List<VitalMeasurement> measurements) {
        if (measurements.isEmpty()) {
            return "Keine Wearable-Daten vorhanden.";
        }

        Map<VitalType, Double> averages = averageByType(measurements);
        StringJoiner sj = new StringJoiner(", ", "Garmin-Vitaldaten (letzte 24 h): ", "");
        averages.forEach((type, avg) -> sj.add(formatVital(type, avg)));
        return sj.toString();
    }

    /**
     * Gibt eine strukturierte Liste von Vitaldaten-Ablesungen zurück, die das
     * Frontend für Farb-Codierung und Dashboard-Karten verwenden kann.
     *
     * @param measurements Rohdaten aus dem Wearable-Repository
     * @return Liste von {@link VitalReading}-Objekten (leer wenn keine Daten)
     */
    public List<VitalReading> summariseStructured(List<VitalMeasurement> measurements) {
        if (measurements.isEmpty()) {
            return List.of();
        }

        Map<VitalType, Double> averages = averageByType(measurements);
        List<VitalReading> readings = new ArrayList<>();

        averages.forEach((type, avg) -> {
            switch (type) {
                case HEART_RATE ->
                        readings.add(new VitalReading("HEART_RATE", "Herzrate",
                                Math.round(avg), "bpm", avg > HR_TACHYKARDIA_THRESHOLD));
                case HRV ->
                        readings.add(new VitalReading("HRV", "HRV",
                                Math.round(avg), "ms", avg < HRV_LOW_THRESHOLD));
                case SPO2 ->
                        readings.add(new VitalReading("SPO2", "SpO₂",
                                Math.round(avg * 10.0) / 10.0, "%", avg < SPO2_BREATHLESSNESS_THRESHOLD));
                case STRESS_LEVEL ->
                        readings.add(new VitalReading("STRESS_LEVEL", "Stress",
                                Math.round(avg), "/100", avg > STRESS_ANXIETY_THRESHOLD));
                case SLEEP_DURATION_HOURS ->
                        readings.add(new VitalReading("SLEEP_DURATION_HOURS", "Schlaf",
                                Math.round(avg * 10.0) / 10.0, "h", avg < SLEEP_FATIGUE_THRESHOLD));
                case STEPS_PER_DAY ->
                        readings.add(new VitalReading("STEPS_PER_DAY", "Schritte",
                                Math.round(avg), "/Tag", false));
            }
        });

        return readings;
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    private Map<VitalType, Double> averageByType(List<VitalMeasurement> measurements) {
        return measurements.stream()
                .collect(Collectors.groupingBy(
                        VitalMeasurement::type,
                        Collectors.averagingDouble(VitalMeasurement::value)));
    }

    private String formatVital(VitalType type, double avg) {
        return switch (type) {
            case HEART_RATE ->
                    "Herzrate ⌀ %.0f bpm%s".formatted(avg, avg > HR_TACHYKARDIA_THRESHOLD ? " [↑]" : "");
            case HRV ->
                    "HRV ⌀ %.0f ms%s".formatted(avg, avg < HRV_LOW_THRESHOLD ? " [↓]" : "");
            case SPO2 ->
                    "SpO₂ ⌀ %.1f%%%s".formatted(avg, avg < SPO2_BREATHLESSNESS_THRESHOLD ? " [↓]" : "");
            case STRESS_LEVEL ->
                    "Stress %d/100%s".formatted(Math.round(avg), avg > STRESS_ANXIETY_THRESHOLD ? " [↑]" : "");
            case STEPS_PER_DAY ->
                    "Schritte ⌀ %.0f/Tag".formatted(avg);
            case SLEEP_DURATION_HOURS ->
                    "Schlaf ⌀ %.1f h%s".formatted(avg, avg < SLEEP_FATIGUE_THRESHOLD ? " [↓]" : "");
        };
    }
}
