package com.promevity.medai.domain;

/**
 * Represents a Patient node in the Neo4j Knowledge Graph.
 *
 * <p>Node label: {@code Patient}
 * <p>Properties: {@code id}, {@code name}, {@code age}
 *
 * <p>Relationships (stored as graph edges, not as Java fields here):
 * <pre>
 *   (Patient)-[:EXPERIENCES {date: datetime}]->(Symptom)
 * </pre>
 *
 * <p>We intentionally keep this as a plain Java record — the Neo4j driver
 * returns raw {@link org.neo4j.driver.Record} objects which we map manually
 * in {@link com.promevity.medai.repository.PatientRepository}.  This avoids
 * pulling in Spring Data Neo4j (SDN) and keeps the PoC dependency footprint
 * minimal while remaining idiomatic Quarkus.
 */
public record Patient(String id, String name, int age) {

    /**
     * Factory method that maps a Neo4j {@link org.neo4j.driver.types.Node}
     * to a {@code Patient} record.
     */
    public static Patient fromNode(org.neo4j.driver.types.Node node) {
        return new Patient(
                node.get("id").asString(),
                node.get("name").asString(),
                node.get("age").asInt()
        );
    }
}
