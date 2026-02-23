package com.promevity.medai.adapter.out.neo4j;

import com.promevity.medai.application.port.out.WearableRepositoryPort;
import com.promevity.medai.domain.model.VitalMeasurement;
import com.promevity.medai.domain.model.VitalType;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Getriebener Adapter — Neo4j-Implementierung von {@link WearableRepositoryPort}.
 *
 * <p><b>Hexagonale Architektur — Adapter OUT / Neo4j</b><br>
 * Speichert Garmin-Vitaldaten als {@code VitalMeasurement}-Knoten im Graphen
 * und verknüpft sie via {@code HAS_MEASUREMENT}-Kante mit dem Patienten.
 *
 * <h2>Graph-Schema</h2>
 * <pre>
 *   (:Patient)-[:HAS_MEASUREMENT]->(:VitalMeasurement {
 *       id, type, value, unit, recordedAt, source
 *   })
 * </pre>
 *
 * <h2>Implementierter Port</h2>
 * {@link WearableRepositoryPort}
 */
@ApplicationScoped
public class Neo4jWearableRepository implements WearableRepositoryPort {

    @Inject
    Driver driver;

    @Override
    public void saveMeasurements(String patientId, List<VitalMeasurement> measurements) {
        String cypher = """
                MATCH (p:Patient {id: $patientId})
                MERGE (v:VitalMeasurement {id: $id})
                ON CREATE SET v.type       = $type,
                              v.value      = $value,
                              v.unit       = $unit,
                              v.recordedAt = $recordedAt,
                              v.source     = 'Garmin'
                MERGE (p)-[:HAS_MEASUREMENT]->(v)
                """;

        try (Session session = session()) {
            session.executeWrite(tx -> {
                for (VitalMeasurement m : measurements) {
                    String id = (m.id() != null && !m.id().isBlank())
                            ? m.id()
                            : "vital-%s-%s".formatted(patientId, UUID.randomUUID());

                    tx.run(cypher, Values.parameters(
                            "patientId",  patientId,
                            "id",         id,
                            "type",       m.type().name(),
                            "value",      m.value(),
                            "unit",       m.unit(),
                            "recordedAt", m.recordedAt().toString()));
                }
                return null;
            });
        }

        Log.infof("[Neo4j] %d VitalMeasurement(s) für Patient %s gespeichert",
                measurements.size(), patientId);
    }

    @Override
    public List<VitalMeasurement> findRecentByPatientId(String patientId, int withinHours) {
        String since = LocalDateTime.now().minusHours(withinHours).toString();

        String cypher = """
                MATCH (p:Patient {id: $patientId})-[:HAS_MEASUREMENT]->(v:VitalMeasurement)
                WHERE v.recordedAt >= $since
                RETURN v.id        AS id,
                       v.type      AS type,
                       v.value     AS value,
                       v.unit      AS unit,
                       v.recordedAt AS recordedAt
                ORDER BY v.recordedAt DESC
                """;

        try (Session session = session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters(
                        "patientId", patientId,
                        "since",     since));
                return result.list(r -> new VitalMeasurement(
                        r.get("id").asString(),
                        VitalType.valueOf(r.get("type").asString()),
                        r.get("value").asDouble(),
                        r.get("unit").asString(),
                        LocalDateTime.parse(r.get("recordedAt").asString())));
            });
        }
    }

    private Session session() {
        return driver.session(SessionConfig.defaultConfig());
    }
}
