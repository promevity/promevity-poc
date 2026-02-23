package com.promevity.medai;

import com.promevity.medai.domain.model.DifferentialDiagnosis;
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
 * <p>Diese Tests benötigen weder Neo4j noch einen LLM-API-Key — die CPT-Daten
 * werden von {@link TestDiseaseKnowledgeRepository} (CDI-Alternative) bereitgestellt.
 * Das ist ein Kernvorteil der Hexagonalen Architektur: Domain-Services sind ohne
 * Infrastruktur vollständig testbar.
 *
 * <h2>CPT-Werte der Test-Wissensbasis (aus TestDiseaseKnowledgeRepository)</h2>
 * <pre>
 *   Thyroid Dysfunction  prior=0.15
 *     Tachykardia: P(s|D)=0.75, P(s|¬D)=0.20
 *     Fatigue:     P(s|D)=0.70, P(s|¬D)=0.40
 *
 *   Anemia               prior=0.12
 *     Fatigue:     P(s|D)=0.90, P(s|¬D)=0.40
 *     Tachykardia: P(s|D)=0.55, P(s|¬D)=0.20
 *
 *   Cardiac Arrhythmia   prior=0.08
 *     Tachykardia: P(s|D)=0.90, P(s|¬D)=0.20
 *
 *   Naïve-Bayes-Ergebnisse (Top-Disease):
 *     {Tachy, Fatigue} → Thyroid  ≈ 54 %  (Top-1)
 *     {Tachy}          → Thyroid  ≈ 40 %  (Top-1)
 *     {Fatigue}        → Thyroid  ≈ 24 %  (Top-1, knapp vor Anemia ≈ 24 %)
 *     {}               → Thyroid  ≈ 15 %  (Top-1 per Prior)
 * </pre>
 */
@QuarkusTest
class BayesianRiskServiceTest {

    @Inject
    BayesianRiskService service;

    private static final Symptom TACHYKARDIA   = new Symptom("symptom-tachykardia",   Symptom.TACHYKARDIA);
    private static final Symptom FATIGUE       = new Symptom("symptom-fatigue",       Symptom.FATIGUE);
    private static final Symptom CHEST_PAIN    = new Symptom("symptom-chest-pain",    Symptom.CHEST_PAIN);
    private static final Symptom BREATHLESSNESS= new Symptom("symptom-breathlessness",Symptom.BREATHLESSNESS);
    private static final Symptom PALLOR        = new Symptom("symptom-pallor",        Symptom.PALLOR);
    private static final Symptom WEAKNESS      = new Symptom("symptom-weakness",      Symptom.WEAKNESS);

    // ── Top-Disease & Probability ─────────────────────────────────────────

    @Test
    @DisplayName("Tachykardia + Fatigue → Top: Thyroid Dysfunction (~54 %)")
    void beideSymptome_thyroidIstTopErkrankung() {
        RiskAssessment result = service.assess(List.of(TACHYKARDIA, FATIGUE));

        assertThat(result.diseaseName()).isEqualTo("Thyroid Dysfunction");
        assertThat(result.probabilityPercentage())
                .as("Beide Symptome → ca. 54 % (Naïve-Bayes, unabhängige CPTs)")
                .isBetween(48.0, 62.0);
        assertThat(result.evidenceSymptoms()).hasSize(2);
    }

    @Test
    @DisplayName("Nur Tachykardia → Top: Thyroid Dysfunction (~40 %)")
    void nurTachykardia_thyroidTop_mittlereWahrscheinlichkeit() {
        RiskAssessment result = service.assess(List.of(TACHYKARDIA));

        assertThat(result.diseaseName()).isEqualTo("Thyroid Dysfunction");
        assertThat(result.probabilityPercentage())
                .as("Nur Tachykardia → ca. 39–41 %")
                .isBetween(35.0, 45.0);
    }

    @Test
    @DisplayName("Nur Fatigue → Top: Thyroid Dysfunction oder Anemia (~24 %)")
    void nurFatigue_thyroidOderAnemiaTop() {
        RiskAssessment result = service.assess(List.of(FATIGUE));

        assertThat(result.diseaseName())
                .as("Bei Fatigue liegt Thyroid und Anemia nahe beieinander")
                .isIn("Thyroid Dysfunction", "Anemia");
        assertThat(result.probabilityPercentage())
                .as("Nur Fatigue → ca. 23–25 %")
                .isBetween(18.0, 30.0);
    }

    @Test
    @DisplayName("Keine Symptome → Top: Thyroid Dysfunction (Prior = 15 %)")
    void keineSymptome_thyroidTop_naheAmPrior() {
        RiskAssessment result = service.assess(List.of());

        assertThat(result.diseaseName()).isEqualTo("Thyroid Dysfunction");
        assertThat(result.probabilityPercentage())
                .as("Ohne Evidenz → Prior von Thyroid (15 %)")
                .isBetween(10.0, 20.0);
        assertThat(result.evidenceSymptoms()).isEmpty();
    }

    // ── Differential Diagnosis Ranking ───────────────────────────────────

    @Test
    @DisplayName("Differential-Liste enthält alle 3 Erkrankungen")
    void differentialListe_enthaeltAlleDreiErkrankungen() {
        RiskAssessment result = service.assess(List.of(TACHYKARDIA, FATIGUE));

        assertThat(result.differentialDiagnoses())
                .as("Alle 3 Erkrankungen aus der Test-Wissensbasis")
                .hasSize(3)
                .extracting(DifferentialDiagnosis::diseaseName)
                .containsExactlyInAnyOrder("Thyroid Dysfunction", "Anemia", "Cardiac Arrhythmia");
    }

    @Test
    @DisplayName("Differential-Liste ist absteigend nach Wahrscheinlichkeit sortiert")
    void differentialListe_absteigendSortiert() {
        RiskAssessment result = service.assess(List.of(TACHYKARDIA, FATIGUE));

        List<Double> probabilities = result.differentialDiagnoses().stream()
                .map(DifferentialDiagnosis::probabilityPercentage)
                .toList();

        for (int i = 0; i < probabilities.size() - 1; i++) {
            assertThat(probabilities.get(i))
                    .as("Eintrag %d muss >= Eintrag %d sein", i, i + 1)
                    .isGreaterThanOrEqualTo(probabilities.get(i + 1));
        }
    }

    @Test
    @DisplayName("Cardiac Arrhythmia rangiert bei {Tachy, Chest Pain, Breathlessness} vorne")
    void arrhythmieSymptome_arrhythmieVorne() {
        RiskAssessment result = service.assess(List.of(TACHYKARDIA, CHEST_PAIN, BREATHLESSNESS));

        assertThat(result.diseaseName()).isEqualTo("Cardiac Arrhythmia");
        assertThat(result.probabilityPercentage())
                .as("Cardiac Arrhythmia spezifische Symptome → deutlich > 50 %")
                .isGreaterThan(50.0);
    }

    @Test
    @DisplayName("Anemia rangiert bei {Fatigue, Pallor, Weakness} vorne")
    void anemieSymptome_anemieVorne() {
        RiskAssessment result = service.assess(List.of(FATIGUE, PALLOR, WEAKNESS));

        assertThat(result.diseaseName()).isEqualTo("Anemia");
        assertThat(result.probabilityPercentage())
                .as("Anemia spezifische Symptome → deutlich > 50 %")
                .isGreaterThan(50.0);
    }

    @Test
    @DisplayName("Top-Disease stimmt immer mit erstem Differential-Eintrag überein")
    void topDisease_stimmtMitDifferentialEins_ueberein() {
        List.of(
                List.of(TACHYKARDIA, FATIGUE),
                List.of(TACHYKARDIA),
                List.of(FATIGUE),
                List.<Symptom>of()
        ).forEach(symptoms -> {
            RiskAssessment result = service.assess(symptoms);
            assertThat(result.diseaseName())
                    .isEqualTo(result.differentialDiagnoses().get(0).diseaseName());
            assertThat(result.probabilityPercentage())
                    .isEqualTo(result.differentialDiagnoses().get(0).probabilityPercentage());
        });
    }
}
