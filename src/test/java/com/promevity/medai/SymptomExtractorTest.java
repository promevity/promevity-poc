package com.promevity.medai;

import com.promevity.medai.application.service.SymptomExtractor;
import com.promevity.medai.domain.model.Symptom;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link SymptomExtractor} (Application Layer).
 *
 * <p>Keine Infrastruktur erforderlich — der Extraktor ist ein reiner
 * In-Memory-Service ohne externe Abhängigkeiten.
 */
@QuarkusTest
class SymptomExtractorTest {

    @Inject
    SymptomExtractor extractor;

    @Test
    @DisplayName("'high heart rate' → Tachykardia")
    void highHeartRate_mapptAufTachykardia() {
        List<Symptom> result = extractor.extract("My smartwatch shows a high heart rate");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo(Symptom.TACHYKARDIA);
    }

    @Test
    @DisplayName("'very tired' → Fatigue")
    void veryTired_mapptAufFatigue() {
        List<Symptom> result = extractor.extract("I feel very tired today");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo(Symptom.FATIGUE);
    }

    @Test
    @DisplayName("Beide Symptome in einem Satz — keine Duplikate")
    void beideSymptome_keineDuplikate() {
        String text = "I feel very tired today and my smartwatch shows a high heart rate";
        List<Symptom> result = extractor.extract(text);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Symptom::name)
                .containsExactlyInAnyOrder(Symptom.TACHYKARDIA, Symptom.FATIGUE);
    }

    @Test
    @DisplayName("Wiederholtes Keyword — nur ein Symptom zurückgegeben")
    void wiederholtesKeyword_dedupliziert() {
        String text = "I am exhausted, I feel very tired, I have fatigue";
        long fatigueCount = extractor.extract(text).stream()
                .filter(s -> s.name().equals(Symptom.FATIGUE))
                .count();

        assertThat(fatigueCount)
                .as("Fatigue darf trotz mehrerer Matches nur einmal erscheinen")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Unbekannter Text → leere Liste")
    void unbekannterText_leereListe() {
        assertThat(extractor.extract("I had a great day at the park")).isEmpty();
    }

    @Test
    @DisplayName("Null, Leerstring, Whitespace → leere Liste")
    void nullOderLeer_leereListe() {
        assertThat(extractor.extract(null)).isEmpty();
        assertThat(extractor.extract("")).isEmpty();
        assertThat(extractor.extract("   ")).isEmpty();
    }

    @Test
    @DisplayName("Groß-/Kleinschreibung wird ignoriert")
    void grossKleinschreibung_ignoriert() {
        List<Symptom> result = extractor.extract("PALPITATION and FATIGUE detected");
        assertThat(result).extracting(Symptom::name)
                .contains(Symptom.TACHYKARDIA, Symptom.FATIGUE);
    }

    @Test
    @DisplayName("'chest pain' → Chest Pain")
    void chestPain_mapptAufChestPain() {
        List<Symptom> result = extractor.extract("I have chest pain and shortness of breath");

        assertThat(result).extracting(Symptom::name)
                .contains(Symptom.CHEST_PAIN, Symptom.BREATHLESSNESS);
    }

    @Test
    @DisplayName("'tremor' und 'weight loss' → Tremor + Weight Loss")
    void tremorUndWeightLoss_erkannt() {
        List<Symptom> result = extractor.extract("I noticed tremor in my hands and weight loss");

        assertThat(result).extracting(Symptom::name)
                .contains(Symptom.TREMOR, Symptom.WEIGHT_LOSS);
    }

    @Test
    @DisplayName("'weakness' → Weakness (nicht Fatigue)")
    void weakness_mapptAufWeakness() {
        List<Symptom> result = extractor.extract("I feel a lot of weakness in my arms");

        assertThat(result).extracting(Symptom::name)
                .contains(Symptom.WEAKNESS)
                .doesNotContain(Symptom.FATIGUE);
    }
}
