package com.promevity.medai.application.port.in;

/**
 * Primärer Port — liefert den vollständigen Patient-Digital-Twin-Graphen.
 *
 * <p><b>Hexagonale Architektur — Application Layer / Port IN</b><br>
 * Dieser Port abstrahiert das Lesen des Knowledge Graphs für UI-Visualisierungen.
 * Der REST-Adapter ruft ihn auf; die konkrete Implementierung liegt im
 * {@code DiagnosticService}.
 */
public interface GetPatientGraphUseCase {

    /**
     * Gibt Knoten und Kanten des Patient-Digital-Twin-Graphen zurück.
     *
     * @param patientId logische Patienten-ID
     * @return {@link PatientGraphResult} mit Patienten- und Symptom-Knoten sowie Kanten
     * @throws jakarta.ws.rs.NotFoundException wenn der Patient nicht existiert
     */
    PatientGraphResult getPatientGraph(String patientId);
}
