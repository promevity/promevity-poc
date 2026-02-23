package com.promevity.medai.application.port.in;

/**
 * Primärer Port — Symptom-Timeline eines Patienten abrufen.
 *
 * <p><b>Hexagonale Architektur — Application Layer / Port IN</b><br>
 * Gibt die chronologische Entwicklung der Diagnosewahrscheinlichkeit zurück:
 * Für jeden historischen Beobachtungszeitpunkt wird berechnet, welche
 * Diagnose mit den bis dahin akkumulierten Symptomen am wahrscheinlichsten war.
 *
 * <p>Das macht das "Gedächtnis des Graphen" sichtbar — ein zentraler
 * Aha-Moment des Personalized Medicine PoC.
 */
public interface GetPatientTimelineUseCase {

    /**
     * Berechnet die Symptom-Timeline für einen Patienten.
     *
     * @param patientId logische Patienten-ID (muss existieren)
     * @return chronologische Timeline mit Bayes-Wahrscheinlichkeiten je Zeitpunkt
     * @throws jakarta.ws.rs.NotFoundException wenn der Patient nicht existiert
     */
    PatientTimelineResult getPatientTimeline(String patientId);
}
