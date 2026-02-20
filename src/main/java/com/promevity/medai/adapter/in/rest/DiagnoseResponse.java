package com.promevity.medai.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Ausgehendes HTTP-Payload von {@code POST /api/diagnose/{patientId}}.
 *
 * <p><b>Hexagonale Architektur — Adapter IN / REST</b><br>
 * Der REST-Adapter mappt das {@code DiagnoseResult} des Use Case
 * in dieses HTTP-spezifische DTO.  So bleibt die Applikationsschicht
 * frei von HTTP/JSON-Belangen.
 */
public record DiagnoseResponse(

        @JsonProperty("patientId")
        String patientId,

        @JsonProperty("detectedSymptoms")
        List<String> detectedSymptoms,

        @JsonProperty("diseaseName")
        String diseaseName,

        @JsonProperty("probabilityPercentage")
        double probabilityPercentage,

        @JsonProperty("explanation")
        String explanation

) {
    /** Mappt ein {@code DiagnoseResult} (Applikationsschicht) auf dieses DTO. */
    public static DiagnoseResponse from(com.promevity.medai.application.port.in.DiagnoseResult result) {
        return new DiagnoseResponse(
                result.patientId(),
                result.allSymptoms(),
                result.diseaseName(),
                result.probabilityPercentage(),
                result.explanation()
        );
    }
}
