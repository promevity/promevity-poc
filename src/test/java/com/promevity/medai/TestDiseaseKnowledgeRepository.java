package com.promevity.medai;

import com.promevity.medai.application.port.out.DiseaseKnowledgePort;
import com.promevity.medai.domain.model.DiseaseCptData;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.List;

/**
 * Test-Implementierung von {@link DiseaseKnowledgePort}.
 *
 * <p>Ersetzt im Test-Kontext den {@code Neo4jDiseaseKnowledgeRepository} durch
 * eine In-Memory-Variante mit denselben CPT-Werten wie das Produktions-Cypher-Skript.
 * So können Unit-Tests des {@code BayesianRiskService} ohne Neo4j-Verbindung ausgeführt werden.
 *
 * <p>{@code @Alternative @Priority(1)} überschreibt die Production-Bean mit höherer Priorität.
 */
@ApplicationScoped
@Alternative
@Priority(1)
class TestDiseaseKnowledgeRepository implements DiseaseKnowledgePort {

    @Override
    public List<DiseaseCptData> loadAll() {
        return List.of(
                new DiseaseCptData("Thyroid Dysfunction", 0.15, List.of(
                        new DiseaseCptData.SymptomLikelihood("Tachykardia",     0.75, 0.20),
                        new DiseaseCptData.SymptomLikelihood("Fatigue",         0.70, 0.40),
                        new DiseaseCptData.SymptomLikelihood("Tremor",          0.50, 0.08),
                        new DiseaseCptData.SymptomLikelihood("Weight Loss",     0.65, 0.10),
                        new DiseaseCptData.SymptomLikelihood("Heat Intolerance",0.70, 0.05),
                        new DiseaseCptData.SymptomLikelihood("Anxiety",         0.55, 0.25)
                )),
                new DiseaseCptData("Anemia", 0.12, List.of(
                        new DiseaseCptData.SymptomLikelihood("Fatigue",         0.90, 0.40),
                        new DiseaseCptData.SymptomLikelihood("Tachykardia",     0.55, 0.20),
                        new DiseaseCptData.SymptomLikelihood("Pallor",          0.75, 0.05),
                        new DiseaseCptData.SymptomLikelihood("Breathlessness",  0.65, 0.15),
                        new DiseaseCptData.SymptomLikelihood("Weakness",        0.80, 0.30)
                )),
                new DiseaseCptData("Cardiac Arrhythmia", 0.08, List.of(
                        new DiseaseCptData.SymptomLikelihood("Tachykardia",     0.90, 0.20),
                        new DiseaseCptData.SymptomLikelihood("Chest Pain",      0.65, 0.10),
                        new DiseaseCptData.SymptomLikelihood("Breathlessness",  0.60, 0.15),
                        new DiseaseCptData.SymptomLikelihood("Fatigue",         0.50, 0.40),
                        new DiseaseCptData.SymptomLikelihood("Anxiety",         0.50, 0.25)
                ))
        );
    }
}
