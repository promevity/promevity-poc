package com.promevity.medai.domain;

/**
 * Represents a Symptom node in the Medical Ontology layer of the Knowledge Graph.
 *
 * <p>Node label: {@code Symptom}
 * <p>Properties: {@code id}, {@code name} (e.g. "Tachykardia", "Fatigue")
 *
 * <p>Symptoms act as the controlled vocabulary / ontology dictionary.
 * Patients are linked to Symptom nodes via {@code [:EXPERIENCES]} edges,
 * making symptom lookup a single graph traversal rather than a free-text search.
 *
 * <p>Ontology expansion tip: In production, replace this simple node with
 * SNOMED-CT or ICD-11 concept identifiers on the {@code externalCode} property.
 */
public record Symptom(String id, String name) {

    /**
     * Well-known symptom names used throughout the PoC.
     * Having them as constants avoids magic strings and makes the
     * Bayesian rule set easy to read.
     */
    public static final String TACHYKARDIA = "Tachykardia";
    public static final String FATIGUE     = "Fatigue";

    /**
     * Factory method that maps a Neo4j {@link org.neo4j.driver.types.Node}
     * to a {@code Symptom} record.
     */
    public static Symptom fromNode(org.neo4j.driver.types.Node node) {
        return new Symptom(
                node.get("id").asString(),
                node.get("name").asString()
        );
    }
}
