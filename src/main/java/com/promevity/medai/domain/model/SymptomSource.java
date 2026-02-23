package com.promevity.medai.domain.model;

/**
 * Value Object — Quelle eines erkannten Symptoms.
 *
 * <p>Zeigt an, woher ein Symptom in der Diagnose-Pipeline stammt:
 * <ul>
 *   <li>{@code TEXT}      — aus dem Patienten-Freitext (NLP)</li>
 *   <li>{@code GARMIN}    — aus Wearable-Vitaldaten abgeleitet</li>
 *   <li>{@code TEXT+GARMIN} — aus beiden Quellen bestätigt</li>
 *   <li>{@code VERLAUF}   — aus der historischen GraphRAG-Vorgeschichte</li>
 * </ul>
 *
 * @param symptomName Name des Symptoms
 * @param source      Herkunftsquelle (TEXT | GARMIN | TEXT+GARMIN | VERLAUF)
 */
public record SymptomSource(String symptomName, String source) {}
