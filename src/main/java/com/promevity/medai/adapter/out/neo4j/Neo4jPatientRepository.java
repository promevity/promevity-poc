package com.promevity.medai.adapter.out.neo4j;

import com.promevity.medai.application.port.out.PatientRepositoryPort;
import com.promevity.medai.domain.model.Patient;
import com.promevity.medai.domain.model.Symptom;
import com.promevity.medai.domain.model.SymptomHistoryEntry;
import com.promevity.medai.domain.model.SymptomObservation;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Getriebener Adapter — Neo4j-Implementierung von {@link PatientRepositoryPort}.
 *
 * <p><b>Hexagonale Architektur — Adapter OUT / Neo4j</b><br>
 * Diese Klasse ist der einzige Ort, an dem Neo4j-spezifischer Code steht:
 * Cypher-Queries, der {@link Driver} und die Node-zu-Record-Mappings.
 * Weder die Domäne noch die Applikationsschicht haben davon Kenntnis.
 *
 * <h2>Wichtige Design-Entscheidung</h2>
 * <p>Wir verwenden den rohen Neo4j-Treiber statt Spring Data Neo4j (SDN),
 * um die Abhängigkeiten minimal zu halten und Cypher explizit lesbar zu machen.
 * Alle Sessions werden mit {@code try-with-resources} geöffnet, damit
 * Verbindungen zuverlässig in den Pool zurückgegeben werden.
 *
 * <h2>Implementierter Port</h2>
 * {@link PatientRepositoryPort}
 */
@ApplicationScoped
public class Neo4jPatientRepository implements PatientRepositoryPort {

    @Inject
    Driver driver;

    // ─────────────────────────────────────────────────────────────────────────
    // PatientRepositoryPort — Patient
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Optional<Patient> findById(String patientId) {
        String cypher = """
                MATCH (p:Patient {id: $id})
                RETURN p
                """;
        try (Session session = session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters("id", patientId));
                if (result.hasNext()) {
                    return Optional.of(toPatient(result.next().get("p").asNode()));
                }
                return Optional.<Patient>empty();
            });
        }
    }

    @Override
    public void save(Patient patient) {
        String cypher = """
                MERGE (p:Patient {id: $id})
                ON CREATE SET p.name = $name, p.age = $age
                ON MATCH  SET p.name = $name, p.age = $age
                """;
        try (Session session = session()) {
            session.executeWrite(tx -> {
                tx.run(cypher, Values.parameters(
                        "id",   patient.id(),
                        "name", patient.name(),
                        "age",  patient.age()));
                return null;
            });
        }
        Log.debugf("[Neo4j] Patient gespeichert [id=%s]", patient.id());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PatientRepositoryPort — Symptom / Relationship
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void saveSymptom(Symptom symptom) {
        String cypher = """
                MERGE (s:Symptom {id: $id})
                ON CREATE SET s.name = $name
                """;
        try (Session session = session()) {
            session.executeWrite(tx -> {
                tx.run(cypher, Values.parameters("id", symptom.id(), "name", symptom.name()));
                return null;
            });
        }
        Log.debugf("[Neo4j] Symptom-Knoten gespeichert [id=%s, name=%s]", symptom.id(), symptom.name());
    }

    @Override
    public void recordSymptom(String patientId, String symptomId, LocalDate date) {
        String cypher = """
                MATCH (p:Patient {id: $patientId})
                MATCH (s:Symptom {id: $symptomId})
                MERGE (p)-[r:EXPERIENCES {date: $date}]->(s)
                RETURN r
                """;
        try (Session session = session()) {
            session.executeWrite(tx -> {
                var result = tx.run(cypher, Values.parameters(
                        "patientId", patientId,
                        "symptomId", symptomId,
                        "date",      date.toString()));
                if (!result.hasNext()) {
                    throw new IllegalStateException(
                            "EXPERIENCES-Kante konnte nicht erstellt werden: Patient [%s] oder Symptom [%s] fehlt"
                                    .formatted(patientId, symptomId));
                }
                return null;
            });
        }
        Log.debugf("[Neo4j] EXPERIENCES-Kante gespeichert [%s → %s @ %s]", patientId, symptomId, date);
    }

    /**
     * GraphRAG-Retrieval: traversiert den Graphen und gibt alle bekannten
     * Symptome des Patienten zurück.
     *
     * <p>Cypher-Pattern:
     * <pre>
     *   MATCH (p:Patient {id: $id})-[:EXPERIENCES]->(s:Symptom)
     *   RETURN DISTINCT s
     * </pre>
     */
    @Override
    public List<Symptom> findSymptomsByPatientId(String patientId) {
        String cypher = """
                MATCH (p:Patient {id: $id})-[:EXPERIENCES]->(s:Symptom)
                RETURN DISTINCT s
                ORDER BY s.name
                """;
        try (Session session = session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters("id", patientId));
                return result.list(record -> toSymptom(record.get("s").asNode()));
            });
        }
    }

    @Override
    public List<SymptomHistoryEntry> findSymptomHistory(String patientId) {
        String cypher = """
                MATCH (p:Patient {id: $id})-[r:EXPERIENCES]->(s:Symptom)
                RETURN s, max(r.date) AS latestDate
                ORDER BY latestDate DESC
                """;
        try (Session session = session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters("id", patientId));
                return result.list(record -> new SymptomHistoryEntry(
                        toSymptom(record.get("s").asNode()),
                        record.get("latestDate").asString("")));
            });
        }
    }

    @Override
    public List<SymptomObservation> findAllSymptomObservations(String patientId) {
        String cypher = """
                MATCH (p:Patient {id: $id})-[r:EXPERIENCES]->(s:Symptom)
                RETURN s, r.date AS observedDate
                ORDER BY observedDate ASC
                """;
        try (Session session = session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters("id", patientId));
                return result.list(record -> new SymptomObservation(
                        toSymptom(record.get("s").asNode()),
                        record.get("observedDate").asString("")));
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hilfsmethoden — Mapping Neo4j-Node → Domänenobjekt
    // ─────────────────────────────────────────────────────────────────────────

    private Patient toPatient(Node node) {
        return new Patient(
                node.get("id").asString(),
                node.get("name").asString(),
                node.get("age").asInt());
    }

    private Symptom toSymptom(Node node) {
        return new Symptom(
                node.get("id").asString(),
                node.get("name").asString());
    }

    private Session session() {
        return driver.session(SessionConfig.defaultConfig());
    }
}
