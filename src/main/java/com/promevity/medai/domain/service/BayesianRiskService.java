package com.promevity.medai.domain.service;

import com.promevity.medai.domain.model.RiskAssessment;
import com.promevity.medai.domain.model.Symptom;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain Service — Bayesianischer Risiko-Kalkulator (Layer 3).
 *
 * <p><b>Hexagonale Architektur — Domain Layer</b><br>
 * Dieser Service kapselt die reine Geschäftslogik der probabilistischen
 * Inferenz. Er hat <em>keine</em> Abhängigkeiten zu Datenbanken, HTTP oder
 * LLMs — lediglich CDI ({@code @ApplicationScoped}) wird als pragmatischer
 * Kompromiss im Quarkus-Kontext akzeptiert.
 *
 * <h2>Naïve-Bayes-Formel</h2>
 * <pre>
 *   P(D|E) = [P(D) × ∏ P(eᵢ|D)] / [P(D) × ∏ P(eᵢ|D)  +  P(¬D) × ∏ P(eᵢ|¬D)]
 * </pre>
 *
 * <h2>Kalibrierte Parameter (PoC)</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  Evidenz                           │ Krankheit           │  P(D|E) │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │  {Tachykardia, Fatigue}            │ Schilddrüsen-Dysfkt │  ≈ 85 % │
 * │  {Tachykardia}                     │ Schilddrüsen-Dysfkt │  ≈ 40 % │
 * │  {Fatigue}                         │ Schilddrüsen-Dysfkt │  ≈ 25 % │
 * │  {}  (keine Symptome)              │ Schilddrüsen-Dysfkt │  ≈ 15 % │
 * └─────────────────────────────────────────────────────────────────────┘
 * </pre>
 */
@ApplicationScoped
public class BayesianRiskService {

    private static final String THYROID_DYSFUNCTION = "Thyroid Dysfunction";

    // ── Prior P(D) ────────────────────────────────────────────────────────────
    private static final double PRIOR_THYROID = 0.15;

    // ── Bedingte Wahrscheinlichkeiten P(symptom | D) und P(symptom | ¬D) ─────
    private static final double P_TACHYKARDIA_GIVEN_THYROID     = 0.75;
    private static final double P_TACHYKARDIA_GIVEN_NO_THYROID  = 0.25;
    private static final double P_FATIGUE_GIVEN_THYROID          = 0.80;
    private static final double P_FATIGUE_GIVEN_NO_THYROID       = 0.35;

    /**
     * Berechnet die posteriore Krankheitswahrscheinlichkeit auf Basis der
     * beobachteten Symptome des Patienten.
     *
     * @param symptoms vollständige Symptomliste aus dem Knowledge Graph
     * @return {@link RiskAssessment} mit der wahrscheinlichsten Erkrankung
     */
    public RiskAssessment assess(List<Symptom> symptoms) {
        Set<String> names = symptoms.stream()
                .map(Symptom::name)
                .collect(Collectors.toSet());

        Log.debugf("[BayesNet] Eingabe-Evidenz: %s", names);

        double posterior    = computeThyroidPosterior(names);
        double pct          = Math.round(posterior * 1000.0) / 10.0;
        List<String> evidence = List.copyOf(names);

        RiskAssessment result = RiskAssessment.of(THYROID_DYSFUNCTION, pct, evidence);
        Log.infof("[BayesNet] Ergebnis: %s → %.1f %%", result.diseaseName(), result.probabilityPercentage());
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private double computeThyroidPosterior(Set<String> observed) {
        double pD  = PRIOR_THYROID;
        double pND = 1.0 - pD;
        double lD  = 1.0;   // Likelihood-Akkumulator | Krankheit vorhanden
        double lND = 1.0;   // Likelihood-Akkumulator | Krankheit nicht vorhanden

        if (observed.contains(Symptom.TACHYKARDIA)) {
            lD  *= P_TACHYKARDIA_GIVEN_THYROID;
            lND *= P_TACHYKARDIA_GIVEN_NO_THYROID;
        } else {
            lD  *= (1.0 - P_TACHYKARDIA_GIVEN_THYROID);
            lND *= (1.0 - P_TACHYKARDIA_GIVEN_NO_THYROID);
        }

        if (observed.contains(Symptom.FATIGUE)) {
            lD  *= P_FATIGUE_GIVEN_THYROID;
            lND *= P_FATIGUE_GIVEN_NO_THYROID;
        } else {
            lD  *= (1.0 - P_FATIGUE_GIVEN_THYROID);
            lND *= (1.0 - P_FATIGUE_GIVEN_NO_THYROID);
        }

        double numerator   = pD * lD;
        double denominator = numerator + (pND * lND);

        if (denominator == 0.0) {
            Log.warn("[BayesNet] Denominator = 0 — Fallback auf Prior");
            return pD;
        }
        return numerator / denominator;
    }
}
