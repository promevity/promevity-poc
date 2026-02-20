package com.promevity.medai.adapter.in.rest;

import com.promevity.medai.application.port.in.DiagnoseResult;
import com.promevity.medai.application.port.in.DiagnoseUseCase;
import com.promevity.medai.application.port.in.RegisterPatientUseCase;
import com.promevity.medai.domain.model.Patient;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Treibender Adapter — REST-Schnittstelle für den Diagnose-Use-Case.
 *
 * <p><b>Hexagonale Architektur — Adapter IN / REST</b><br>
 * Diese Klasse ist ein <em>treibender Adapter</em>: Sie übersetzt
 * HTTP-Anfragen in Use-Case-Aufrufe und Use-Case-Ergebnisse zurück
 * in HTTP-Antworten.
 *
 * <p>Sie kennt <em>ausschließlich</em> die primären Port-Interfaces:
 * <ul>
 *   <li>{@link DiagnoseUseCase}</li>
 *   <li>{@link RegisterPatientUseCase}</li>
 * </ul>
 * Die konkreten Implementierungen ({@code DiagnosticService}, Neo4j-Adapter,
 * LLM-Adapter) sind dem REST-Adapter vollständig verborgen.
 *
 * <h2>Endpunkte</h2>
 * <pre>
 *   POST /api/diagnose/{patientId}   — vollständiger Diagnose-Workflow
 *   POST /api/diagnose/patient       — Patienten anlegen (PoC-Hilfsendpunkt)
 * </pre>
 */
@Path("/api/diagnose")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Diagnose", description = "GraphRAG + Bayes + LLM Diagnosepipeline")
public class DiagnosticResource {

    /** Primärer Port — Diagnose-Workflow */
    @Inject
    DiagnoseUseCase diagnoseUseCase;

    /** Primärer Port — Patientenregistrierung */
    @Inject
    RegisterPatientUseCase registerPatientUseCase;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/diagnose/{patientId}
    // ─────────────────────────────────────────────────────────────────────────

    @POST
    @Path("/{patientId}")
    @Operation(
            summary = "KI-Diagnosepipeline ausführen",
            description = """
                    Verarbeitet freien Patiententext, extrahiert Symptome (NLP),
                    persistiert sie im Neo4j Knowledge Graph, berechnet die
                    Bayesianische Risikowahrscheinlichkeit und liefert eine
                    patientengerechte LLM-Erklärung zurück.
                    """
    )
    @APIResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = DiagnoseResponse.class)))
    @APIResponse(responseCode = "400", description = "Ungültiger Request-Body")
    @APIResponse(responseCode = "404", description = "Patient nicht gefunden")
    public Response diagnose(
            @Parameter(description = "Logische Patienten-ID", required = true)
            @PathParam("patientId") String patientId,
            @Valid DiagnoseRequest request
    ) {
        Log.infof("[REST] POST /api/diagnose/%s", patientId);

        // Adapter → Use Case: nur Primitive/Records übergeben
        DiagnoseResult result = diagnoseUseCase.diagnose(patientId, request.rawText());

        // Use-Case-Ergebnis → HTTP-DTO mappen
        return Response.ok(DiagnoseResponse.from(result)).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/diagnose/patient
    // ─────────────────────────────────────────────────────────────────────────

    @POST
    @Path("/patient")
    @Operation(summary = "Patienten im Knowledge Graph anlegen oder aktualisieren")
    @APIResponse(responseCode = "201", description = "Patient erfolgreich gespeichert")
    public Response createPatient(Patient patient) {
        Log.infof("[REST] POST /api/diagnose/patient [id=%s]", patient.id());
        Patient saved = registerPatientUseCase.register(patient);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }
}
