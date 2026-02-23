package com.promevity.medai.domain.model;

import java.time.LocalDateTime;

/**
 * Value Object — ein einzelner Vitaldaten-Messpunkt aus einem Wearable.
 *
 * <p><b>Hexagonale Architektur — Domain Layer</b><br>
 * Repräsentiert eine konkrete Messung (z. B. Herzrate 108 bpm um 07:32 Uhr).
 * Wird als {@code VitalMeasurement}-Knoten im Neo4j-Graphen gespeichert und
 * über eine {@code HAS_MEASUREMENT}-Kante mit dem Patienten verknüpft.
 *
 * <p>Neo4j-Node-Label: {@code VitalMeasurement}<br>
 * Properties: {@code id}, {@code type}, {@code value}, {@code unit},
 * {@code recordedAt}, {@code source}
 *
 * @param id         eindeutige ID (vom Repository generiert, falls {@code null})
 * @param type       Typ der Messung (z. B. {@link VitalType#HEART_RATE})
 * @param value      Messwert als Double
 * @param unit       Maßeinheit (fällt auf {@link VitalType#defaultUnit()} zurück)
 * @param recordedAt Zeitstempel der Messung (ISO-8601)
 */
public record VitalMeasurement(
        String        id,
        VitalType     type,
        double        value,
        String        unit,
        LocalDateTime recordedAt
) {}
