package com.promevity.medai.application.port.in;

/**
 * Primärer Port (Driving Port) — "Diagnose durchführen".
 *
 * <p><b>Hexagonale Architektur — Application Layer / Port IN</b><br>
 * Dieser Port definiert den <em>Vertrag</em> zwischen dem treibenden Adapter
 * (z. B. REST, CLI, gRPC) und dem Anwendungskern.  Der Adapter kennt nur
 * dieses Interface — niemals die konkrete Implementierung.
 *
 * <p>Implementiert von: {@code DiagnosticService}
 * <br>Aufgerufen von:   {@code DiagnosticResource} (REST-Adapter)
 */
public interface DiagnoseUseCase {

    /**
     * Führt den vollständigen 6-stufigen Diagnose-Workflow aus:
     * NLP-Extraktion → Neo4j-Persistenz → GraphRAG → Bayes → LLM → Antwort.
     *
     * @param patientId die logische Patienten-ID (Neo4j {@code id}-Property)
     * @param rawText   freier Text des Patienten (z. B. Beschwerden)
     * @return strukturiertes Diagnoseergebnis inkl. LLM-Erklärung
     * @throws jakarta.ws.rs.NotFoundException wenn der Patient unbekannt ist
     */
    DiagnoseResult diagnose(String patientId, String rawText);
}
