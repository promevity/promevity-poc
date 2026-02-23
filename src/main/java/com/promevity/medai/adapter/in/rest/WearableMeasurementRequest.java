package com.promevity.medai.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.promevity.medai.domain.model.VitalType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Eingehendes HTTP-Payload für einen einzelnen Vitaldaten-Messpunkt.
 *
 * <p><b>Hexagonale Architektur — Adapter IN / REST</b><br>
 * Wird als JSON-Array im Body von {@code POST /api/wearable/{patientId}/measurements}
 * erwartet.
 *
 * <h2>Beispiel-Payload</h2>
 * <pre>{@code
 * [
 *   { "type": "HEART_RATE", "value": 108.0, "recordedAt": "2026-02-22T07:30:00" },
 *   { "type": "HRV",        "value":  24.0, "recordedAt": "2026-02-22T07:30:00" },
 *   { "type": "SPO2",       "value":  98.0, "recordedAt": "2026-02-22T07:31:00" }
 * ]
 * }</pre>
 *
 * @param type        Vitaldaten-Typ (z. B. {@code HEART_RATE}, {@code HRV}, {@code SPO2})
 * @param value       Messwert
 * @param unit        Maßeinheit (optional — Fallback: {@link VitalType#defaultUnit()})
 * @param recordedAt  Messzeitpunkt (ISO-8601, z. B. {@code 2026-02-22T07:30:00})
 */
public record WearableMeasurementRequest(

        @JsonProperty("type")
        @NotNull
        VitalType type,

        @JsonProperty("value")
        double value,

        @JsonProperty("unit")
        String unit,

        @JsonProperty("recordedAt")
        @NotNull
        LocalDateTime recordedAt

) {}
