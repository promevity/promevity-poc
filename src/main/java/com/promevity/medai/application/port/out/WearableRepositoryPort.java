package com.promevity.medai.application.port.out;

import com.promevity.medai.domain.model.VitalMeasurement;

import java.util.List;

/**
 * Sekundärer Port (Driven Port) — Persistenz und Abfrage von Wearable-Vitaldaten.
 *
 * <p><b>Hexagonale Architektur — Application Layer / Port OUT</b><br>
 * Der Anwendungskern kennt nur dieses Interface — nicht die konkrete
 * Neo4j-Implementierung dahinter.  Adapter: {@code Neo4jWearableRepository}.
 */
public interface WearableRepositoryPort {

    /**
     * Speichert eine Liste von Vitaldaten-Messpunkten für einen Patienten.
     * Verwendet MERGE-Semantik — idempotent bei gleicher ID.
     *
     * @param patientId    ID des Patienten (muss existieren)
     * @param measurements zu speichernde Messungen
     */
    void saveMeasurements(String patientId, List<VitalMeasurement> measurements);

    /**
     * Lädt die jüngsten Vitaldaten-Messpunkte eines Patienten.
     *
     * @param patientId   ID des Patienten
     * @param withinHours Zeitfenster in Stunden (rückwirkend ab jetzt)
     * @return Liste der Messungen, absteigend nach {@code recordedAt} sortiert
     */
    List<VitalMeasurement> findRecentByPatientId(String patientId, int withinHours);
}
