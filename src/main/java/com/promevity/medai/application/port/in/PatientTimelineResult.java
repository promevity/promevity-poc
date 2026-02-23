package com.promevity.medai.application.port.in;

import java.util.List;

/**
 * Output-DTO des {@link GetPatientTimelineUseCase}.
 *
 * <p>Enthält die chronologische Diagnosehistorie eines Patienten:
 * Für jedes Datum, an dem neue Symptome beobachtet wurden, wird die
 * kumulierte Bayes-Wahrscheinlichkeit berechnet und zurückgegeben.
 * So lässt sich visualisieren, wie der Knowledge Graph über die Zeit
 * ein "Gedächtnis" aufbaut und die Diagnose schrittweise präzisiert.
 *
 * @param patientId   logische Patienten-ID
 * @param patientName Anzeigename
 * @param points      chronologische Zeitreihenpunkte (aufsteigend nach Datum)
 */
public record PatientTimelineResult(
        String patientId,
        String patientName,
        List<TimelinePoint> points
) {

    /**
     * Ein einzelner Zeitpunkt in der Diagnose-Timeline.
     *
     * @param date                   ISO-8601-Datum (z. B. "2025-01-15")
     * @param addedSymptoms          Symptome, die an diesem Datum neu hinzukamen
     * @param allSymptoms            kumulierte Symptome bis einschließlich dieses Datums
     * @param topDisease             wahrscheinlichste Diagnose mit diesen Symptomen
     * @param probabilityPercentage  Posteriori-Wahrscheinlichkeit (0–100, 1 Dezimalstelle)
     */
    public record TimelinePoint(
            String date,
            List<String> addedSymptoms,
            List<String> allSymptoms,
            String topDisease,
            double probabilityPercentage
    ) {}
}
