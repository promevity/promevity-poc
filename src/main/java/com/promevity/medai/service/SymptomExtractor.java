package com.promevity.medai.service;

import com.promevity.medai.domain.Symptom;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * NLP Symptom Extractor — mocked for the PoC.
 *
 * <p>Translates free-form patient text into a list of {@link Symptom} objects
 * by scanning for well-known keyword patterns.  In a production system, this
 * would be replaced by:
 * <ul>
 *   <li>A dedicated NER (Named Entity Recognition) model fine-tuned on
 *       clinical text (e.g., Med7 or BioBERT), OR</li>
 *   <li>An LLM call with structured output (JSON mode) that returns a list
 *       of SNOMED-CT concept IDs.</li>
 * </ul>
 *
 * <h2>Current mapping table</h2>
 * <pre>
 * ┌──────────────────────────────────────┬──────────────────────┐
 * │  Input keywords (case-insensitive)   │  Canonical Symptom   │
 * ├──────────────────────────────────────┼──────────────────────┤
 * │  high heart rate, tachycardia,       │  Tachykardia         │
 * │  racing heart, palpitations,         │                      │
 * │  heart racing, fast heartbeat        │                      │
 * ├──────────────────────────────────────┼──────────────────────┤
 * │  tired, fatigue, exhausted,          │  Fatigue             │
 * │  very tired, low energy,             │                      │
 * │  weakness, sluggish                  │                      │
 * └──────────────────────────────────────┴──────────────────────┘
 * </pre>
 */
@ApplicationScoped
public class SymptomExtractor {

    /**
     * Keyword → canonical symptom name mapping.
     * Keys are lowercase fragments; matching is done with
     * {@link String#contains(CharSequence)} for simplicity.
     */
    private static final Map<String, String> KEYWORD_MAP = Map.ofEntries(
            // Tachycardia variants
            Map.entry("high heart rate",  Symptom.TACHYKARDIA),
            Map.entry("tachycardia",      Symptom.TACHYKARDIA),
            Map.entry("tachykardia",      Symptom.TACHYKARDIA),
            Map.entry("racing heart",     Symptom.TACHYKARDIA),
            Map.entry("heart racing",     Symptom.TACHYKARDIA),
            Map.entry("palpitation",      Symptom.TACHYKARDIA),
            Map.entry("fast heartbeat",   Symptom.TACHYKARDIA),
            Map.entry("rapid heartbeat",  Symptom.TACHYKARDIA),
            Map.entry("rapid pulse",      Symptom.TACHYKARDIA),

            // Fatigue variants
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
     * Extracts a deduplicated list of {@link Symptom} objects from raw text.
     *
     * <p>Returns an empty list if no known keywords are found, allowing the
     * Bayesian engine to evaluate the patient's baseline (prior) risk.
     *
     * @param rawText free-form text from the patient or clinician
     * @return list of distinct matched symptoms
     */
    public List<Symptom> extract(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        String normalised = rawText.toLowerCase(Locale.ROOT);
        List<Symptom> detected = new ArrayList<>();

        for (Map.Entry<String, String> entry : KEYWORD_MAP.entrySet()) {
            String keyword      = entry.getKey();
            String symptomName  = entry.getValue();

            if (normalised.contains(keyword)) {
                boolean alreadyAdded = detected.stream()
                        .anyMatch(s -> s.name().equals(symptomName));
                if (!alreadyAdded) {
                    // Generate a deterministic ID from the symptom name so that
                    // MERGE in Neo4j is idempotent across extractions.
                    String id = "symptom-" + symptomName.toLowerCase(Locale.ROOT)
                                                        .replace(" ", "-");
                    detected.add(new Symptom(id, symptomName));
                    Log.debugf("Extracted symptom '%s' via keyword '%s'", symptomName, keyword);
                }
            }
        }

        Log.infof("Extraction complete — found %d symptom(s) in text", detected.size());
        return List.copyOf(detected);
    }
}
