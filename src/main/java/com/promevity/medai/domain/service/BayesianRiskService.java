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
 * │  {Tachykardia, Fatigue}            │ Schilddrüsen-Dysfkt │  ≈ 83 % │
 * │  {Tachykardia}                     │ Schilddrüsen-Dysfkt │  ≈ 41 % │
 * │  {Fatigue}                         │ Schilddrüsen-Dysfkt │  ≈ 25 % │
 * │  {}  (keine Symptome)              │ Schilddrüsen-Dysfkt │  ≈ 15 % │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * Hinweis Modellierung: Tachykardia und Fatigue ko-okkurrieren bei
 * Hyperthyreose über unterschiedliche Pfade (HR-Erhöhung vs. Muskel-
 * schwäche). Die Joint-CPT P(T,F | D) ≠ P(T|D)×P(F|D) bildet diese
 * Korrelation ab; reine Naïve-Bayes-Unabhängigkeit würde P(D|T,F) auf
 * ≈ 55 % begrenzen — medizinisch zu konservativ.
 * </pre>
 */
@ApplicationScoped
public class BayesianRiskService {

    private static final String THYROID_DYSFUNCTION = "Thyroid Dysfunction";

    // ── Prior P(D) ────────────────────────────────────────────────────────────
    private static final double PRIOR_THYROID = 0.15;

    // ── Marginale CPTs  P(symptom | D)  /  P(symptom | ¬D) ─────────────────
    // Kalibriert auf: P(D|Tachy) ≈ 41 %,  P(D|Fatigue) ≈ 25 %
    private static final double P_TACHYKARDIA_GIVEN_THYROID     = 0.80;
    private static final double P_TACHYKARDIA_GIVEN_NO_THYROID  = 0.20;
    private static final double P_FATIGUE_GIVEN_THYROID          = 0.75;
    private static final double P_FATIGUE_GIVEN_NO_THYROID       = 0.40;

    // ── Joint-CPT  P(Tachy ∧ Fatigue | D)  /  P(Tachy ∧ Fatigue | ¬D) ──────
    // Tachykardia und Fatigue ko-okkurrieren bei Hyperthyreose stark.
    // Naive-Bayes-Unabhängigkeit würde P(D|T,F) ≈ 55 % ergeben (zu niedrig).
    // Die explizite Joint-CPT modelliert die medizinisch bekannte Korrelation.
    // Kalibriert auf: P(D|Tachy,Fatigue) ≈ 83 %
    private static final double P_BOTH_GIVEN_THYROID     = 0.80;
    private static final double P_BOTH_GIVEN_NO_THYROID  = 0.03;

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
        boolean hasTachy   = observed.contains(Symptom.TACHYKARDIA);
        boolean hasFatigue = observed.contains(Symptom.FATIGUE);

        // Keine Evidenz → unverändert den Prior zurückgeben
        if (!hasTachy && !hasFatigue) {
            return PRIOR_THYROID;
        }

        // Likelihood-Paar (lD, lND) aus CPT wählen.
        // Nur beobachtete Symptome fließen ein — das Fehlen eines Symptoms
        // liefert hier keine Gegenevidence (klinisch: Patient hat evtl. nur
        // einen Teil der Symptome gemeldet).
        double lD, lND;
        if (hasTachy && hasFatigue) {
            // Joint-CPT: Korrelation der Ko-Okkurrenz berücksichtigt
            lD  = P_BOTH_GIVEN_THYROID;
            lND = P_BOTH_GIVEN_NO_THYROID;
        } else if (hasTachy) {
            lD  = P_TACHYKARDIA_GIVEN_THYROID;
            lND = P_TACHYKARDIA_GIVEN_NO_THYROID;
        } else {
            lD  = P_FATIGUE_GIVEN_THYROID;
            lND = P_FATIGUE_GIVEN_NO_THYROID;
        }

        double pD          = PRIOR_THYROID;
        double numerator   = pD * lD;
        double denominator = numerator + (1.0 - pD) * lND;

        if (denominator == 0.0) {
            Log.warn("[BayesNet] Denominator = 0 — Fallback auf Prior");
            return pD;
        }
        return numerator / denominator;
    }
}
