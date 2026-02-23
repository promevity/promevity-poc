package com.promevity.medai.adapter.in.rest;

import com.promevity.medai.application.port.in.IngestWearableDataUseCase;
import com.promevity.medai.domain.model.VitalMeasurement;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST-Adapter — Wearable-Dateneingabe (treibender Adapter / Adapter IN).
 *
 * <p><b>Hexagonale Architektur — Adapter IN / REST</b><br>
 * Nimmt Garmin-Vitaldaten entgegen, konvertiert sie in Domänenobjekte
 * und delegiert an {@link IngestWearableDataUseCase}.
 *
 * <h2>Endpunkt</h2>
 * <pre>
 *   POST /api/wearable/{patientId}/measurements
 * </pre>
 * Body: JSON-Array von {@link WearableMeasurementRequest}-Objekten.
 *
 * <h2>Ablauf im Demo</h2>
 * <ol>
 *   <li>Garmin-Daten posten (dieser Endpunkt) → Graphknoten angelegt.</li>
 *   <li>Diagnose-Endpunkt aufrufen → Vitaldaten fließen automatisch ein.</li>
 * </ol>
 */
@Path("/api/wearable")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WearableResource {

    @Inject
    IngestWearableDataUseCase ingestWearableDataUseCase;

    /**
     * Nimmt eine Liste von Vitaldaten-Messungen entgegen und speichert sie
     * im Digital-Twin-Graphen des Patienten.
     *
     * @param patientId ID des Zielpatienten (muss in Neo4j existieren)
     * @param requests  Liste der Messungen (mind. 1)
     * @return 202 Accepted mit Zusammenfassung
     */
    @POST
    @Path("/{patientId}/measurements")
    public Response ingest(
            @PathParam("patientId") String patientId,
            @Valid List<WearableMeasurementRequest> requests) {

        List<VitalMeasurement> measurements = requests.stream()
                .map(r -> new VitalMeasurement(
                        null,
                        r.type(),
                        r.value(),
                        r.unit() != null ? r.unit() : r.type().defaultUnit(),
                        r.recordedAt()))
                .toList();

        ingestWearableDataUseCase.ingest(patientId, measurements);

        return Response.accepted(new IngestResponse(patientId, measurements.size())).build();
    }

    /** Antwort-DTO für den Wearable-Ingest-Endpunkt. */
    public record IngestResponse(
            String patientId,
            int    measurementsStored
    ) {}
}
