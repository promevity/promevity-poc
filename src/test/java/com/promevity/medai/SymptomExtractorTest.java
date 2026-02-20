package com.promevity.medai;

import com.promevity.medai.domain.Symptom;
import com.promevity.medai.service.SymptomExtractor;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SymptomExtractor}.
 *
 * <p>Verifies keyword-to-symptom mapping and deduplication logic.
 * No external infrastructure required.
 */
@QuarkusTest
class SymptomExtractorTest {

    @Inject
    SymptomExtractor extractor;

    @Test
    @DisplayName("High heart rate phrase → Tachykardia")
    void highHeartRate_mapsToTachykardia() {
        List<Symptom> result = extractor.extract(
                "My smartwatch shows a high heart rate all day");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo(Symptom.TACHYKARDIA);
    }

    @Test
    @DisplayName("Tired phrase → Fatigue")
    void tired_mapsToFatigue() {
        List<Symptom> result = extractor.extract("I feel very tired today");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo(Symptom.FATIGUE);
    }

    @Test
    @DisplayName("Both symptoms in one sentence — no duplicates")
    void bothSymptoms_noDuplicates() {
        String text = "I feel very tired today and my smartwatch shows a high heart rate";
        List<Symptom> result = extractor.extract(text);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Symptom::name)
                .containsExactlyInAnyOrder(Symptom.TACHYKARDIA, Symptom.FATIGUE);
    }

    @Test
    @DisplayName("Repeated keyword — only one Symptom returned")
    void repeatedKeyword_deduplicated() {
        String text = "I am exhausted, I feel very tired, and I have fatigue";
        List<Symptom> result = extractor.extract(text);

        long fatigueCount = result.stream()
                .filter(s -> s.name().equals(Symptom.FATIGUE))
                .count();
        assertThat(fatigueCount)
                .as("Fatigue should appear exactly once despite multiple matching keywords")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Unknown text → empty list")
    void unknownText_emptyResult() {
        List<Symptom> result = extractor.extract("I had a great day at the park");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Null or blank input → empty list")
    void nullOrBlank_emptyResult() {
        assertThat(extractor.extract(null)).isEmpty();
        assertThat(extractor.extract("   ")).isEmpty();
        assertThat(extractor.extract("")).isEmpty();
    }

    @Test
    @DisplayName("Case-insensitive matching")
    void caseInsensitive_matches() {
        List<Symptom> result = extractor.extract("PALPITATION and FATIGUE");
        assertThat(result).extracting(Symptom::name)
                .contains(Symptom.TACHYKARDIA, Symptom.FATIGUE);
    }
}
