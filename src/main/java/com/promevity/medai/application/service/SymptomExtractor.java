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
 * ┌────────────────────────────────────────┬──────────────────────────┐
 * │  Eingabe-Schlüsselwort (lowercase)     │  Kanonisches Symptom     │
 * ├────────────────────────────────────────┼──────────────────────────┤
 * │  high heart rate, tachycardia,         │  Tachykardia             │
 * │  racing heart, palpitation, …          │                          │
 * ├────────────────────────────────────────┼──────────────────────────┤
 * │  tired, fatigue, exhausted,            │  Fatigue                 │
 * │  low energy, sluggish, lethargic, …    │                          │
 * ├────────────────────────────────────────┼──────────────────────────┤
 * │  tremor, shaking, trembling, …         │  Tremor                  │
 * ├────────────────────────────────────────┼──────────────────────────┤
 * │  weight loss, losing weight, …         │  Weight Loss             │
 * ├────────────────────────────────────────┼──────────────────────────┤
 * │  heat intolerance, feeling hot, …      │  Heat Intolerance        │
 * ├────────────────────────────────────────┼──────────────────────────┤
 * │  anxiety, anxious, nervous, …          │  Anxiety                 │
 * ├────────────────────────────────────────┼──────────────────────────┤
 * │  pallor, pale skin, pale, …            │  Pallor                  │
 * ├────────────────────────────────────────┼──────────────────────────┤
 * │  breathless, short of breath, …        │  Breathlessness          │
 * ├────────────────────────────────────────┼──────────────────────────┤
 * │  weakness, weak, muscle weakness, …    │  Weakness                │
 * ├────────────────────────────────────────┼──────────────────────────┤
 * │  chest pain, chest pressure, …         │  Chest Pain              │
 * └────────────────────────────────────────┴──────────────────────────┘
 * </pre>
 */
@ApplicationScoped
public class SymptomExtractor {

    private static final Map<String, String> KEYWORD_MAP = Map.ofEntries(
            // Tachykardia-Varianten
            Map.entry("high heart rate",     Symptom.TACHYKARDIA),
            Map.entry("tachycardia",         Symptom.TACHYKARDIA),
            Map.entry("tachykardia",         Symptom.TACHYKARDIA),
            Map.entry("racing heart",        Symptom.TACHYKARDIA),
            Map.entry("heart racing",        Symptom.TACHYKARDIA),
            Map.entry("palpitation",         Symptom.TACHYKARDIA),
            Map.entry("fast heartbeat",      Symptom.TACHYKARDIA),
            Map.entry("rapid heartbeat",     Symptom.TACHYKARDIA),
            Map.entry("rapid pulse",         Symptom.TACHYKARDIA),
            // Fatigue-Varianten
            Map.entry("very tired",          Symptom.FATIGUE),
            Map.entry("feel tired",          Symptom.FATIGUE),
            Map.entry("feeling tired",       Symptom.FATIGUE),
            Map.entry("tired",               Symptom.FATIGUE),
            Map.entry("fatigue",             Symptom.FATIGUE),
            Map.entry("exhausted",           Symptom.FATIGUE),
            Map.entry("low energy",          Symptom.FATIGUE),
            Map.entry("sluggish",            Symptom.FATIGUE),
            Map.entry("lethargic",           Symptom.FATIGUE),
            // Tremor-Varianten
            Map.entry("tremor",              Symptom.TREMOR),
            Map.entry("shaking",             Symptom.TREMOR),
            Map.entry("hand tremor",         Symptom.TREMOR),
            Map.entry("trembling",           Symptom.TREMOR),
            Map.entry("shaky hands",         Symptom.TREMOR),
            // Weight Loss-Varianten
            Map.entry("weight loss",         Symptom.WEIGHT_LOSS),
            Map.entry("losing weight",       Symptom.WEIGHT_LOSS),
            Map.entry("lost weight",         Symptom.WEIGHT_LOSS),
            Map.entry("unexplained weight",  Symptom.WEIGHT_LOSS),
            // Heat Intolerance-Varianten
            Map.entry("heat intolerance",    Symptom.HEAT_INTOLERANCE),
            Map.entry("feeling hot",         Symptom.HEAT_INTOLERANCE),
            Map.entry("overheated",          Symptom.HEAT_INTOLERANCE),
            Map.entry("can't stand heat",    Symptom.HEAT_INTOLERANCE),
            Map.entry("heat sensitive",      Symptom.HEAT_INTOLERANCE),
            // Anxiety-Varianten
            Map.entry("anxiety",             Symptom.ANXIETY),
            Map.entry("anxious",             Symptom.ANXIETY),
            Map.entry("feeling anxious",     Symptom.ANXIETY),
            Map.entry("nervous",             Symptom.ANXIETY),
            Map.entry("panic",               Symptom.ANXIETY),
            // Pallor-Varianten
            Map.entry("pallor",              Symptom.PALLOR),
            Map.entry("pale skin",           Symptom.PALLOR),
            Map.entry("skin pale",           Symptom.PALLOR),
            Map.entry("paleness",            Symptom.PALLOR),
            Map.entry("looking pale",        Symptom.PALLOR),
            // Breathlessness-Varianten
            Map.entry("breathless",          Symptom.BREATHLESSNESS),
            Map.entry("breathlessness",      Symptom.BREATHLESSNESS),
            Map.entry("short of breath",     Symptom.BREATHLESSNESS),
            Map.entry("shortness of breath", Symptom.BREATHLESSNESS),
            Map.entry("difficulty breathing",Symptom.BREATHLESSNESS),
            Map.entry("can't breathe",       Symptom.BREATHLESSNESS),
            // Weakness-Varianten
            Map.entry("weakness",            Symptom.WEAKNESS),
            Map.entry("muscle weakness",     Symptom.WEAKNESS),
            Map.entry("feeling weak",        Symptom.WEAKNESS),
            Map.entry("weak muscles",        Symptom.WEAKNESS),
            // Chest Pain-Varianten
            Map.entry("chest pain",          Symptom.CHEST_PAIN),
            Map.entry("chest pressure",      Symptom.CHEST_PAIN),
            Map.entry("chest tightness",     Symptom.CHEST_PAIN),
            Map.entry("chest discomfort",    Symptom.CHEST_PAIN),
            Map.entry("heart pain",          Symptom.CHEST_PAIN)
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
