package com.promevity.medai.application.service;

import com.promevity.medai.domain.model.Symptom;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Application Service — NLP-Symptom-Extraktor (gemockt für den PoC).
 *
 * <p><b>Hexagonale Architektur — Application Layer</b><br>
 * Gehört zur Applikationsschicht, weil es sich um Use-Case-Logik handelt
 * (Transformation von Rohtext in Domänenobjekte), die keine
 * Infrastruktur-Abhängigkeiten hat.  In Produktion würde dieser Service
 * durch einen eigenen sekundären Port ersetzt, hinter dem ein NER-Modell
 * oder ein LLM mit Structured Output steckt.
 *
 * <h2>Aktuelle Keyword-Tabelle</h2>
 * <pre>
 * ┌────────────────────────────────────────┬──────────────────────┐
 * │  Eingabe-Schlüsselwort (lowercase)     │  Kanonisches Symptom │
 * ├────────────────────────────────────────┼──────────────────────┤
 * │  high heart rate, tachycardia,         │  Tachykardia         │
 * │  racing heart, palpitation, …          │                      │
 * ├────────────────────────────────────────┼──────────────────────┤
 * │  tired, fatigue, exhausted,            │  Fatigue             │
 * │  low energy, sluggish, lethargic, …    │                      │
 * └────────────────────────────────────────┴──────────────────────┘
 * </pre>
 */
@ApplicationScoped
public class SymptomExtractor {

    private static final Map<String, String> KEYWORD_MAP = Map.ofEntries(
            // Tachykardia-Varianten
            Map.entry("high heart rate",  Symptom.TACHYKARDIA),
            Map.entry("tachycardia",      Symptom.TACHYKARDIA),
            Map.entry("tachykardia",      Symptom.TACHYKARDIA),
            Map.entry("racing heart",     Symptom.TACHYKARDIA),
            Map.entry("heart racing",     Symptom.TACHYKARDIA),
            Map.entry("palpitation",      Symptom.TACHYKARDIA),
            Map.entry("fast heartbeat",   Symptom.TACHYKARDIA),
            Map.entry("rapid heartbeat",  Symptom.TACHYKARDIA),
            Map.entry("rapid pulse",      Symptom.TACHYKARDIA),
            // Fatigue-Varianten
            Map.entry("very tired",       Symptom.FATIGUE),
            Map.entry("feel tired",       Symptom.FATIGUE),
            Map.entry("feeling tired",    Symptom.FATIGUE),
            Map.entry("tired",            Symptom.FATIGUE),
            Map.entry("fatigue",          Symptom.FATIGUE),
            Map.entry("exhausted",        Symptom.FATIGUE),
            Map.entry("low energy",       Symptom.FATIGUE),
            Map.entry("weakness",         Symptom.FATIGUE),
            Map.entry("sluggish",         Symptom.FATIGUE),
            Map.entry("lethargic",        Symptom.FATIGUE)
    );

    /**
     * Extrahiert deduplizierte {@link Symptom}-Objekte aus freiem Text.
     *
     * @param rawText Freitext des Patienten oder der Klinik
     * @return unveränderliche Liste erkannter Symptome (kann leer sein)
     */
    public List<Symptom> extract(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        String normalised = rawText.toLowerCase(Locale.ROOT);
        List<Symptom> detected = new ArrayList<>();

        for (Map.Entry<String, String> entry : KEYWORD_MAP.entrySet()) {
            String keyword     = entry.getKey();
            String symptomName = entry.getValue();

            if (normalised.contains(keyword)) {
                boolean alreadyAdded = detected.stream()
                        .anyMatch(s -> s.name().equals(symptomName));
                if (!alreadyAdded) {
                    String id = "symptom-" + symptomName.toLowerCase(Locale.ROOT)
                                                        .replace(" ", "-");
                    detected.add(new Symptom(id, symptomName));
                    Log.debugf("[NLP] Symptom '%s' erkannt via Keyword '%s'", symptomName, keyword);
                }
            }
        }

        Log.infof("[NLP] %d Symptom(e) extrahiert", detected.size());
        return List.copyOf(detected);
    }
}
