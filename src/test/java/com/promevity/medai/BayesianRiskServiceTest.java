package com.promevity.medai;

import com.promevity.medai.domain.Symptom;
import com.promevity.medai.service.BayesianRiskService;
import com.promevity.medai.service.RiskAssessment;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit-level tests for {@link BayesianRiskService}.
 *
 * <p>These tests do NOT require Neo4j or an LLM API key — they only exercise
 * the Bayesian computation logic in isolation, making them fast and suitable
 * for CI pipelines without any infrastructure.
 *
 * <p>Expected probability ranges are derived from the Naïve-Bayes formula
 * implemented in {@link BayesianRiskService}:
 * <pre>
 *   P(ThyroidDysfunction) = 0.15
 *   P(Tachykardia | Thyroid) = 0.75,  P(Tachykardia | ¬Thyroid) = 0.25
 *   P(Fatigue     | Thyroid) = 0.80,  P(Fatigue     | ¬Thyroid) = 0.35
 *
 *   Both present:   P(D|E) ≈ 0.15 × 0.75 × 0.80 / Z ≈ 85%
 *   Tachykardia:    P(D|E) ≈ 0.15 × 0.75 × 0.20 / Z ≈ 39-40%
 *   Fatigue only:   P(D|E) ≈ 0.15 × 0.25 × 0.80 / Z ≈ 24-25%
 *   No symptoms:    P(D|E) ≈ 0.15 × 0.25 × 0.20 / Z ≈ 15%
 * </pre>
 */
@QuarkusTest
class BayesianRiskServiceTest {

    @Inject
    BayesianRiskService bayesianRiskService;

    private static final Symptom TACHYKARDIA =
            new Symptom("symptom-tachykardia", Symptom.TACHYKARDIA);
    private static final Symptom FATIGUE =
            new Symptom("symptom-fatigue", Symptom.FATIGUE);

    @Test
    @DisplayName("Both Tachykardia + Fatigue → high probability (~85%)")
    void bothSymptoms_highRisk() {
        RiskAssessment result = bayesianRiskService.assess(List.of(TACHYKARDIA, FATIGUE));

        assertThat(result.diseaseName()).isEqualTo("Thyroid Dysfunction");
        assertThat(result.probabilityPercentage())
                .as("Both symptoms should yield ~85% probability")
                .isGreaterThan(80.0)
                .isLessThan(92.0);
        assertThat(result.evidenceSymptoms()).hasSize(2);
    }

    @Test
    @DisplayName("Tachykardia only → moderate probability (~40%)")
    void tachykardiaOnly_moderateRisk() {
        RiskAssessment result = bayesianRiskService.assess(List.of(TACHYKARDIA));

        assertThat(result.probabilityPercentage())
                .as("Tachykardia alone should yield ~39-41% probability")
                .isBetween(35.0, 45.0);
    }

    @Test
    @DisplayName("Fatigue only → low-moderate probability (~25%)")
    void fatigueOnly_lowModerateRisk() {
        RiskAssessment result = bayesianRiskService.assess(List.of(FATIGUE));

        assertThat(result.probabilityPercentage())
                .as("Fatigue alone should yield ~24-26% probability")
                .isBetween(20.0, 30.0);
    }

    @Test
    @DisplayName("No symptoms → near-prior probability (~15%)")
    void noSymptoms_nearPrior() {
        RiskAssessment result = bayesianRiskService.assess(List.of());

        assertThat(result.probabilityPercentage())
                .as("No evidence should produce a probability close to the prior (15%)")
                .isBetween(10.0, 20.0);
        assertThat(result.evidenceSymptoms()).isEmpty();
    }

    @Test
    @DisplayName("Result disease name is always Thyroid Dysfunction")
    void diseaseName_alwaysThyroid() {
        List.of(
                List.of(TACHYKARDIA, FATIGUE),
                List.of(TACHYKARDIA),
                List.of(FATIGUE),
                List.<Symptom>of()
        ).forEach(symptoms -> {
            RiskAssessment result = bayesianRiskService.assess(symptoms);
            assertThat(result.diseaseName()).isEqualTo("Thyroid Dysfunction");
        });
    }
}
