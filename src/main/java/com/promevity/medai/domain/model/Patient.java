package com.promevity.medai.domain.model;

/**
 * Aggregate Root — repräsentiert einen Patienten im Knowledge Graph.
 *
 * <p><b>Hexagonale Architektur — Domain Layer</b><br>
 * Diese Klasse enthält ausschließlich fachliche Konzepte und hat keinerlei
 * Abhängigkeiten zu Frameworks (kein CDI, kein Jackson, kein Neo4j).
 * Sie ist damit vollständig framework-agnostisch und isoliert testbar.
 *
 * <p>Neo4j-Node-Label: {@code Patient}<br>
 * Properties: {@code id}, {@code name}, {@code age}
 *
 * <p>Die Abbildung zwischen Neo4j-Records und diesem Domänenobjekt übernimmt
 * ausschließlich der Adapter {@code Neo4jPatientRepository} in der
 * Infrastructure-Schicht.
 */
public record Patient(String id, String name, int age) {

    /** Kompakter Guard gegen leere IDs beim Erzeugen des Aggregats. */
    public Patient {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Patient id must not be blank");
        }
    }
}
