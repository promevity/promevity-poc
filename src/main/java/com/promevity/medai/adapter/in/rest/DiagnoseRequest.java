package com.promevity.medai.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Eingehendes HTTP-Payload für {@code POST /api/diagnose/{patientId}}.
 *
 * <p><b>Hexagonale Architektur — Adapter IN / REST</b><br>
 * Dieses DTO existiert <em>nur</em> in der Adapter-Schicht.  Jackson-
 * und Bean-Validation-Annotationen sind bewusst hier platziert und
 * nicht in der Domäne.
 */
public record DiagnoseRequest(

        @JsonProperty("rawText")
        @NotBlank(message = "rawText darf nicht leer sein")
        @Size(max = 2000, message = "rawText darf maximal 2000 Zeichen lang sein")
        String rawText

) {}
