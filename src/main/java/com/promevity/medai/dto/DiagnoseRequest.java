package com.promevity.medai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload for {@code POST /api/diagnose/{patientId}}.
 *
 * <p>The {@code rawText} field contains free-form natural language entered by
 * the patient or clinician (e.g. "I feel very tired and my heart is racing").
 * The LLM / NLP layer extracts structured symptoms from this text.
 */
public record DiagnoseRequest(

        @JsonProperty("rawText")
        @NotBlank(message = "rawText must not be blank")
        @Size(max = 2000, message = "rawText must be 2000 characters or fewer")
        String rawText

) {}
