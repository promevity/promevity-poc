package com.promevity.medai.repository;

import com.promevity.medai.domain.Patient;
import com.promevity.medai.domain.Symptom;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.NoSuchRecordException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for {@link Patient} and the associated graph relationships.
 *
 * <p>Uses the raw Neo4j {@link Driver} (injected by the {@code quarkus-neo4j}
 * extension) to execute parameterised Cypher queries.  All sessions are opened
 * with {@code try-with-resources} so connections are always returned to the pool.
 *
 * <h2>Graph schema (relevant portion)</h2>
 * <pre>
 *   (:Patient {id, name, age})
 *          |
 *    [:EXPERIENCES {date}]
 *          |
 *          v
 *   (:Symptom {id, name})
 * </pre>
 *
 * <h2>Design decision — no Spring Data Neo4j (SDN)</h2>
 * We deliberately avoid SDN to keep the dependency graph lean for a PoC and to
 * demonstrate readable, explicit Cypher rather than annotation magic.
 */
@ApplicationScoped
public class PatientRepository {

    /** The CDI-managed Neo4j driver bean provided by {@code quarkus-neo4j}. */
    @Inject
    Driver driver;

    // ─────────────────────────────────────────────────────────────────────────
    // Patient CRUD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Looks up a patient by their application-level {@code id} property.
     *
     * @param patientId the patient's logical identifier (not the Neo4j internal id)
     * @return an {@link Optional} containing the patient, or empty if not found
     */
    public Optional<Patient> findById(String patientId) {
        String cypher = """
                MATCH (p:Patient {id: $id})
                RETURN p
                """;

        try (Session session = driver.session(defaultSessionConfig())) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters("id", patientId));
                if (result.hasNext()) {
                    var record = result.next();
                    return Optional.of(Patient.fromNode(record.get("p").asNode()));
                }
                return Optional.<Patient>empty();
            });
        }
    }

    /**
     * Creates a Patient node if it does not already exist (idempotent MERGE).
     *
     * @param patient patient to persist
     */
    public void save(Patient patient) {
        String cypher = """
                MERGE (p:Patient {id: $id})
                ON CREATE SET p.name = $name, p.age = $age
                ON MATCH  SET p.name = $name, p.age = $age
                """;

        try (Session session = driver.session(defaultSessionConfig())) {
            session.executeWrite(tx -> {
                tx.run(cypher, Values.parameters(
                        "id",   patient.id(),
                        "name", patient.name(),
                        "age",  patient.age()
                ));
                return null;
            });
        }
        Log.debugf("Upserted Patient node [id=%s]", patient.id());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Symptom / Relationship operations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ensures a Symptom node exists in the Ontology layer (idempotent MERGE).
     *
     * @param symptom the symptom to create or locate
     */
    public void saveSymptom(Symptom symptom) {
        String cypher = """
                MERGE (s:Symptom {id: $id})
                ON CREATE SET s.name = $name
                """;

        try (Session session = driver.session(defaultSessionConfig())) {
            session.executeWrite(tx -> {
                tx.run(cypher, Values.parameters("id", symptom.id(), "name", symptom.name()));
                return null;
            });
        }
        Log.debugf("Upserted Symptom node [id=%s, name=%s]", symptom.id(), symptom.name());
    }

    /**
     * Records that a patient is experiencing a symptom on a given date.
     *
     * <p>The relationship {@code (Patient)-[:EXPERIENCES {date}]->(Symptom)} is
     * created idempotently: re-submitting the same symptom on the same date is
     * a no-op.  This mirrors the "append-only digital twin" pattern.
     *
     * @param patientId  logical patient id
     * @param symptomId  logical symptom id
     * @param date       observation date (defaults to today if null)
     */
    public void recordSymptom(String patientId, String symptomId, LocalDate date) {
        LocalDate observedOn = (date != null) ? date : LocalDate.now();

        String cypher = """
                MATCH (p:Patient  {id: $patientId})
                MATCH (s:Symptom  {id: $symptomId})
                MERGE (p)-[r:EXPERIENCES {date: $date}]->(s)
                RETURN r
                """;

        try (Session session = driver.session(defaultSessionConfig())) {
            session.executeWrite(tx -> {
                var result = tx.run(cypher, Values.parameters(
                        "patientId", patientId,
                        "symptomId", symptomId,
                        "date",      observedOn.toString()
                ));
                if (!result.hasNext()) {
                    throw new IllegalStateException(
                            "Could not record symptom: Patient [%s] or Symptom [%s] not found"
                                    .formatted(patientId, symptomId));
                }
                return null;
            });
        }
        Log.debugf("Recorded EXPERIENCES relationship [patient=%s -> symptom=%s @ %s]",
                patientId, symptomId, observedOn);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GraphRAG — symptom retrieval (the "R" in GraphRAG)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves <em>all</em> distinct symptoms currently linked to a patient.
     *
     * <p>This is the core <strong>GraphRAG retrieval</strong> step: instead of
     * embedding a free-text patient record, we traverse the structured graph to
     * get a precise, up-to-date symptom list that is then fed to the Bayesian
     * engine and the LLM context window.
     *
     * <p>Cypher pattern:
     * <pre>
     *   MATCH (p:Patient {id: $id})-[:EXPERIENCES]->(s:Symptom)
     *   RETURN DISTINCT s
     * </pre>
     *
     * @param patientId logical patient id
     * @return list of distinct symptoms; empty list if the patient has none
     */
    public List<Symptom> findSymptomsByPatientId(String patientId) {
        String cypher = """
                MATCH (p:Patient {id: $id})-[:EXPERIENCES]->(s:Symptom)
                RETURN DISTINCT s
                ORDER BY s.name
                """;

        try (Session session = driver.session(defaultSessionConfig())) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters("id", patientId));
                return result.list(record -> Symptom.fromNode(record.get("s").asNode()));
            });
        }
    }

    /**
     * Fetches symptoms observed within a given date range (useful for
     * longitudinal analysis — not wired into the PoC endpoint yet).
     *
     * @param patientId logical patient id
     * @param from      start date (inclusive)
     * @param to        end date (inclusive)
     * @return list of symptoms observed in the window
     */
    public List<Symptom> findSymptomsByPatientIdAndDateRange(
            String patientId, LocalDate from, LocalDate to) {

        String cypher = """
                MATCH (p:Patient {id: $id})-[r:EXPERIENCES]->(s:Symptom)
                WHERE r.date >= $from AND r.date <= $to
                RETURN DISTINCT s
                ORDER BY s.name
                """;

        try (Session session = driver.session(defaultSessionConfig())) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters(
                        "id",   patientId,
                        "from", from.toString(),
                        "to",   to.toString()
                ));
                return result.list(record -> Symptom.fromNode(record.get("s").asNode()));
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Default session configuration — reads/writes go to the default database.
     * Swap to {@code SessionConfig.forDatabase("medai")} if you create a named DB.
     */
    private SessionConfig defaultSessionConfig() {
        return SessionConfig.defaultConfig();
    }
}
