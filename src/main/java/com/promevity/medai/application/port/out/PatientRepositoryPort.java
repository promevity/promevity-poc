package com.promevity.medai.application.port.out;

import com.promevity.medai.domain.model.Patient;
import com.promevity.medai.domain.model.Symptom;
import com.promevity.medai.domain.model.SymptomHistoryEntry;
import com.promevity.medai.domain.model.SymptomObservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Sekundärer Port (Driven Port) — Persistenz des Patienten-Knowledge-Graphs.
 *
 * <p><b>Hexagonale Architektur — Application Layer / Port OUT</b><br>
 * Dieses Interface ist der einzige Berührungspunkt zwischen dem Anwendungskern
 * und der Datenbankschicht.  Der Kern kennt <em>nur</em> dieses Interface —
 * kein Neo4j-Treiber, keine Cypher-Queries.
 *
 * <p>Implementiert von: {@code Neo4jPatientRepository} (Adapter/Out)
 * <br>Aufgerufen von:   {@code DiagnosticService}    (Application/Service)
 */
public interface PatientRepositoryPort {

    /** Sucht einen Patienten anhand seiner logischen ID. */
    Optional<Patient> findById(String patientId);

    /** Legt einen Patienten idempotent an (MERGE-Semantik). */
    void save(Patient patient);

    /** Legt einen Symptom-Ontologie-Knoten idempotent an. */
    void saveSymptom(Symptom symptom);

    /**
     * Erstellt eine {@code (Patient)-[:EXPERIENCES {date}]->(Symptom)}-Kante
     * im Knowledge Graph (idempotent).
     */
    void recordSymptom(String patientId, String symptomId, LocalDate date);

    /**
     * GraphRAG-Retrieval: gibt alle bekannten Symptome eines Patienten zurück.
     *
     * <p>Dies ist das Herzstück der Graph-Retrieval-Augmented-Generation:
     * statt Freitext-Embedding wird die strukturierte Graphtraversierung
     * genutzt, um präzise, aktuelle Evidenz für das Bayes-Netz zu liefern.
     */
    List<Symptom> findSymptomsByPatientId(String patientId);

    /**
     * Graph-Visualisierung: gibt alle Symptom-Einträge eines Patienten
     * inklusive des letzten Beobachtungsdatums zurück.
     *
     * <p>Absteigend sortiert nach {@code latestDate}, damit neueste Symptome
     * im Digital-Twin-Graph zuerst erscheinen.
     */
    List<SymptomHistoryEntry> findSymptomHistory(String patientId);

    /**
     * Timeline-Retrieval: gibt alle einzelnen EXPERIENCES-Beziehungen eines Patienten
     * zurück — je Kante eine Zeile (nicht dedupliziert), chronologisch aufsteigend.
     *
     * <p>Ermöglicht die Rekonstruktion des zeitlichen Verlaufs, um die kumulierte
     * Diagnosewahrscheinlichkeit Schritt für Schritt darzustellen.
     */
    List<SymptomObservation> findAllSymptomObservations(String patientId);
}
