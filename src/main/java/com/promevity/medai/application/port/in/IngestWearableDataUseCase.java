package com.promevity.medai.application.port.in;

import com.promevity.medai.domain.model.VitalMeasurement;

import java.util.List;

/**
 * Primärer Port (Driving Port) — Vitaldaten eines Wearables in den Digital Twin einpflegen.
 *
 * <p><b>Hexagonale Architektur — Application Layer / Port IN</b><br>
 * Wird vom REST-Adapter aufgerufen, wenn neue Garmin-Messdaten eintreffen.
 * Implementiert von: {@code DiagnosticService}.
 */
public interface IngestWearableDataUseCase {

    /**
     * Persistiert Vitaldaten-Messungen für einen Patienten im Knowledge Graph.
     *
     * @param patientId    ID des Zielpatienten
     * @param measurements zu speichernde Vitaldaten
     * @throws jakarta.ws.rs.NotFoundException wenn der Patient nicht existiert
     */
    void ingest(String patientId, List<VitalMeasurement> measurements);
}
