package com.promevity.medai.resource;

import com.promevity.medai.domain.Patient;
import com.promevity.medai.domain.Symptom;
import com.promevity.medai.dto.DiagnoseRequest;
import com.promevity.medai.dto.DiagnoseResponse;
import com.promevity.medai.llm.MedicalAssistant;
import com.promevity.medai.repository.PatientRepository;
import com.promevity.medai.service.BayesianRiskService;
import com.promevity.medai.service.RiskAssessment;
import com.promevity.medai.service.SymptomExtractor;
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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST entry point for the Personalized Medicine AI diagnostic pipeline.
 *
 * <h2>Endpoint</h2>
 * <pre>POST /api/diagnose/{patientId}</pre>
 *
 * <h2>Full pipeline (6 steps)</h2>
 * <ol>
 *   <li><b>Extract</b> — {@link SymptomExtractor} maps raw text → {@link Symptom} list.</li>
 *   <li><b>Persist</b> — Detected symptoms + relationships are written to Neo4j via
 *       {@link PatientRepository} (idempotent MERGE statements).</li>
 *   <li><b>Retrieve (GraphRAG)</b> — All historical symptoms for the patient are
 *       fetched from the Knowledge Graph.</li>
 *   <li><b>Infer</b> — {@link BayesianRiskService} computes the posterior disease
 *       probability from the full symptom evidence set.</li>
 *   <li><b>Explain</b> — {@link MedicalAssistant} (LLM) translates the
 *       {@link RiskAssessment} into a patient-friendly text.</li>
 *   <li><b>Respond</b> — Returns a structured {@link DiagnoseResponse} JSON payload.</li>
 * </ol>
 *
 * <h2>Error handling</h2>
 * <ul>
 *   <li>404 if the patient ID is not found in Neo4j.</li>
 *   <li>400 if the request body is missing or blank.</li>
 *   <li>500 for unexpected Neo4j / LLM failures (with logged stack trace).</li>
 * </ul>
 */
@Path("/api/diagnose")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Diagnostic Pipeline", description = "GraphRAG + Bayesian + LLM diagnostic workflow")
public class DiagnosticResource {

    @Inject
    PatientRepository patientRepository;

    @Inject
    SymptomExtractor symptomExtractor;

    @Inject
    BayesianRiskService bayesianRiskService;

    @Inject
    MedicalAssistant medicalAssistant;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/diagnose/{patientId}
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Runs the full 6-step diagnostic pipeline for a given patient.
     *
     * @param patientId the patient's logical identifier (Neo4j {@code id} property)
     * @param request   JSON body containing the patient's free-form text
     * @return a {@link DiagnoseResponse} with symptoms, risk score, and LLM explanation
     */
    @POST
    @Path("/{patientId}")
    @Operation(
            summary = "Run the AI diagnostic pipeline",
            description = """
                    Accepts raw patient text, extracts symptoms via NLP, persists them in
                    the Neo4j Knowledge Graph, computes a Bayesian risk score, and returns
                    a patient-friendly LLM explanation.
                    """
    )
    @APIResponse(
            responseCode = "200",
            description = "Diagnostic result",
            content = @Content(schema = @Schema(implementation = DiagnoseResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "404", description = "Patient not found")
    @APIResponse(responseCode = "500", description = "Internal pipeline error")
    public Response diagnose(
            @Parameter(description = "Patient logical ID", required = true)
            @PathParam("patientId") String patientId,

            @Valid DiagnoseRequest request
    ) {
        Log.infof("=== Diagnostic pipeline START [patientId=%s] ===", patientId);

        // ── Step 0: Validate patient exists ───────────────────────────────────
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> {
                    Log.warnf("Patient not found: %s", patientId);
                    return new NotFoundException("Patient with id '%s' not found".formatted(patientId));
                });

        Log.debugf("Found patient: %s (age %d)", patient.name(), patient.age());

        // ── Step 1: Extract symptoms from raw text ─────────────────────────────
        List<Symptom> extractedSymptoms = symptomExtractor.extract(request.rawText());
        Log.infof("Step 1 — Extracted %d symptom(s): %s",
                extractedSymptoms.size(),
                extractedSymptoms.stream().map(Symptom::name).toList());

        // ── Step 2: Persist symptoms + relationships in Neo4j ─────────────────
        for (Symptom symptom : extractedSymptoms) {
            patientRepository.saveSymptom(symptom);                          // MERGE Symptom node
            patientRepository.recordSymptom(patientId, symptom.id(), LocalDate.now()); // MERGE edge
        }
        Log.infof("Step 2 — Persisted %d symptom(s) to Knowledge Graph", extractedSymptoms.size());

        // ── Step 3: GraphRAG — fetch full symptom history from the graph ───────
        List<Symptom> allSymptoms = patientRepository.findSymptomsByPatientId(patientId);
        Log.infof("Step 3 — GraphRAG retrieved %d cumulative symptom(s) from graph", allSymptoms.size());

        // ── Step 4: Bayesian inference ─────────────────────────────────────────
        RiskAssessment assessment = bayesianRiskService.assess(allSymptoms);
        Log.infof("Step 4 — Bayesian result: %s @ %.1f%%",
                assessment.diseaseName(), assessment.probabilityPercentage());

        // ── Step 5: LLM explanation ────────────────────────────────────────────
        String symptomsForLlm = allSymptoms.stream()
                .map(Symptom::name)
                .collect(Collectors.joining(", "));

        String explanation = medicalAssistant.explain(
                patient.name(),
                symptomsForLlm,
                assessment.diseaseName(),
                assessment.probabilityPercentage(),
                request.rawText()
        );
        Log.infof("Step 5 — LLM explanation generated (%d chars)", explanation.length());

        // ── Step 6: Build and return response ─────────────────────────────────
        DiagnoseResponse response = new DiagnoseResponse(
                patientId,
                allSymptoms.stream().map(Symptom::name).toList(),
                assessment.diseaseName(),
                assessment.probabilityPercentage(),
                explanation
        );

        Log.infof("=== Diagnostic pipeline END [patientId=%s] ===", patientId);
        return Response.ok(response).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/diagnose/patient  — convenience: create a patient on the fly
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates (or upserts) a patient node in Neo4j.
     *
     * <p>This endpoint is provided so the PoC can be exercised end-to-end without
     * pre-populating the database. In production, patient creation would go through
     * a dedicated registration service.
     *
     * @param patient the patient to create
     * @return 201 Created with the saved patient
     */
    @POST
    @Path("/patient")
    @Operation(summary = "Create or upsert a Patient node in Neo4j")
    @APIResponse(responseCode = "201", description = "Patient created/updated")
    public Response createPatient(Patient patient) {
        patientRepository.save(patient);
        Log.infof("Patient upserted [id=%s, name=%s]", patient.id(), patient.name());
        return Response.status(Response.Status.CREATED).entity(patient).build();
    }
}
