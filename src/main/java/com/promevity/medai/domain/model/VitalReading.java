package com.promevity.medai.domain.model;

/**
 * Value Object — ein strukturierter Vitaldaten-Messwert für das Frontend-Dashboard.
 *
 * <p>Im Gegensatz zum Freitext-{@code vitalSummary} enthält dieses Record
 * maschinenlesbare Felder, die das Frontend für Farb-Codierung und Gauge-Darstellung
 * nutzen kann.
 *
 * @param type  Technischer Typ (z. B. {@code HEART_RATE})
 * @param label Anzeigebeschriftung (z. B. {@code Herzrate})
 * @param value Durchschnittswert der letzten 24 h
 * @param unit  Einheit (z. B. {@code bpm}, {@code ms}, {@code %})
 * @param alert {@code true}, wenn der Wert einen klinischen Schwellenwert überschreitet
 */
public record VitalReading(String type, String label, double value, String unit, boolean alert) {}
