package com.promevity.medai.application.port.in;

import java.util.List;

/**
 * Output-DTO des {@link GetPatientGraphUseCase}.
 *
 * <p><b>Hexagonale Architektur — Application Layer / Port IN</b><br>
 * Enthält alle Daten, die der treibende Adapter (REST) benötigt, um den
 * Patient-Digital-Twin-Graphen im Browser zu rendern.
 *
 * @param patientId   logische Patienten-ID
 * @param patientName Anzeigename
 * @param patientAge  Alter in Jahren
 * @param symptoms    Symptomhistorie mit letztem Beobachtungsdatum
 */
public record PatientGraphResult(
        String patientId,
        String patientName,
        int patientAge,
        List<SymptomEntry> symptoms
) {

    /**
     * Ein Symptom-Eintrag aus dem Knowledge Graph.
     *
     * @param id         Symptom-ID (Neo4j-Knoten-ID)
     * @param name       Anzeigename (Ontologie-Term)
     * @param latestDate letztes Beobachtungsdatum (ISO-8601)
     */
    public record SymptomEntry(String id, String name, String latestDate) {}
}
