package com.promevity.medai.adapter.out.neo4j;

import com.promevity.medai.application.port.out.DiseaseKnowledgePort;
import com.promevity.medai.domain.model.DiseaseCptData;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Getriebener Adapter — Neo4j-Implementierung von {@link DiseaseKnowledgePort}.
 *
 * <p><b>Hexagonale Architektur — Adapter OUT / Neo4j</b><br>
 * Liest die medizinische Ontologie (Disease-Knoten + HAS_SYMPTOM_LIKELIHOOD-Kanten)
 * aus dem Knowledge Graph und baut daraus {@link DiseaseCptData}-Objekte für
 * die Bayesianische Inferenz.
 *
 * <h2>Cypher-Pattern</h2>
 * <pre>
 *   MATCH (d:Disease)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s:Symptom)
 *   RETURN d.name, d.prior, s.name, r.p_given_disease, r.p_given_no_disease
 *   ORDER BY d.name, s.name
 * </pre>
 */
@ApplicationScoped
public class Neo4jDiseaseKnowledgeRepository implements DiseaseKnowledgePort {

    @Inject
    Driver driver;

    private static final String CYPHER = """
            MATCH (d:Disease)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s:Symptom)
            RETURN d.name        AS diseaseName,
                   d.prior       AS prior,
                   s.name        AS symptomName,
                   r.p_given_disease    AS pD,
                   r.p_given_no_disease AS pND
            ORDER BY d.name, s.name
            """;

    @Override
    public List<DiseaseCptData> loadAll() {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            return session.executeRead(tx -> {
                var result = tx.run(CYPHER);

                // Group rows by disease name (LinkedHashMap preserves insertion order)
                Map<String, DiseaseAccumulator> acc = new LinkedHashMap<>();

                result.list().forEach(record -> {
                    String diseaseName = record.get("diseaseName").asString();
                    double prior       = record.get("prior").asDouble();
                    String symptomName = record.get("symptomName").asString();
                    double pD          = record.get("pD").asDouble();
                    double pND         = record.get("pND").asDouble();

                    acc.computeIfAbsent(diseaseName, k -> new DiseaseAccumulator(diseaseName, prior))
                            .add(new DiseaseCptData.SymptomLikelihood(symptomName, pD, pND));
                });

                List<DiseaseCptData> diseases = acc.values().stream()
                        .map(DiseaseAccumulator::build)
                        .toList();

                Log.debugf("[Neo4j] %d Erkrankungen mit CPT-Daten geladen", diseases.size());
                return diseases;
            });
        } catch (Exception e) {
            Log.errorf(e, "[Neo4j] Fehler beim Laden der CPT-Daten — fallback auf leere Liste");
            return List.of();
        }
    }

    // ── Helper: accumulates symptom likelihoods while reading rows ────────

    private static final class DiseaseAccumulator {
        private final String diseaseName;
        private final double prior;
        private final List<DiseaseCptData.SymptomLikelihood> likelihoods = new ArrayList<>();

        DiseaseAccumulator(String diseaseName, double prior) {
            this.diseaseName = diseaseName;
            this.prior       = prior;
        }

        void add(DiseaseCptData.SymptomLikelihood sl) {
            likelihoods.add(sl);
        }

        DiseaseCptData build() {
            return new DiseaseCptData(diseaseName, prior, List.copyOf(likelihoods));
        }
    }
}
