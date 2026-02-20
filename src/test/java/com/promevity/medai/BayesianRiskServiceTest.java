package com.promevity.medai;

import com.promevity.medai.domain.model.RiskAssessment;
import com.promevity.medai.domain.model.Symptom;
import com.promevity.medai.domain.service.BayesianRiskService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link BayesianRiskService} (Domain Layer).
 *
 * <p>Diese Tests benötigen weder Neo4j noch einen LLM-API-Key — sie prüfen
 * ausschließlich die isolierte Bayesianische Inferenz-Logik.  Das ist
 * ein Kernvorteil der Hexagonalen Architektur: Domain-Services sind ohne
 * Infrastruktur vollständig testbar.
 *
 * <h2>Erwartete Wahrscheinlichkeitsbereiche (aus der Naïve-Bayes-Formel)</h2>
 * <pre>
 *   Prior P(D) = 0.15
 *   P(Tachy | D) = 0.75,  P(Tachy | ¬D) = 0.25
 *   P(Fat   | D) = 0.80,  P(Fat   | ¬D) = 0.35
 *
 *   Beide:        ≈ 85 %
 *   Nur Tachy:    ≈ 39–41 %
 *   Nur Fatigue:  ≈ 24–26 %
 *   Keine:        ≈ 10–20 %  (nahe am Prior)
 * </pre>
 */
@QuarkusTest
class BayesianRiskServiceTest {

    @Inject
    BayesianRiskService service;

    private static final Symptom TACHYKARDIA = new Symptom("symptom-tachykardia", Symptom.TACHYKARDIA);
    private static final Symptom FATIGUE     = new Symptom("symptom-fatigue",     Symptom.FATIGUE);

    @Test
    @DisplayName("Tachykardia + Fatigue → hohe Wahrscheinlichkeit (~85 %)")
    void beideSymptome_hoheRisikowahrscheinlichkeit() {
        RiskAssessment result = service.assess(List.of(TACHYKARDIA, FATIGUE));

        assertThat(result.diseaseName()).isEqualTo("Thyroid Dysfunction");
        assertThat(result.probabilityPercentage())
                .as("Beide Symptome → ca. 85 %")
                .isGreaterThan(80.0)
                .isLessThan(92.0);
        assertThat(result.evidenceSymptoms()).hasSize(2);
    }

    @Test
    @DisplayName("Nur Tachykardia → mittlere Wahrscheinlichkeit (~40 %)")
    void nurTachykardia_mittlereWahrscheinlichkeit() {
        RiskAssessment result = service.assess(List.of(TACHYKARDIA));

        assertThat(result.probabilityPercentage())
                .as("Nur Tachykardia → ca. 39–41 %")
                .isBetween(35.0, 45.0);
    }

    @Test
    @DisplayName("Nur Fatigue → niedrig-mittlere Wahrscheinlichkeit (~25 %)")
    void nurFatigue_niedrigMittlereWahrscheinlichkeit() {
        RiskAssessment result = service.assess(List.of(FATIGUE));

        assertThat(result.probabilityPercentage())
                .as("Nur Fatigue → ca. 24–26 %")
                .isBetween(20.0, 30.0);
    }

    @Test
    @DisplayName("Keine Symptome → nahe am Prior (~15 %)")
    void keineSymptome_naheAmPrior() {
        RiskAssessment result = service.assess(List.of());

        assertThat(result.probabilityPercentage())
                .as("Ohne Evidenz → nahe am Prior (15 %)")
                .isBetween(10.0, 20.0);
        assertThat(result.evidenceSymptoms()).isEmpty();
    }

    @Test
    @DisplayName("Krankheitsname ist immer 'Thyroid Dysfunction'")
    void krankheitsname_immerThyroid() {
        List.of(
                List.of(TACHYKARDIA, FATIGUE),
                List.of(TACHYKARDIA),
                List.of(FATIGUE),
                List.<Symptom>of()
        ).forEach(symptoms ->
                assertThat(service.assess(symptoms).diseaseName())
                        .isEqualTo("Thyroid Dysfunction"));
    }
}
