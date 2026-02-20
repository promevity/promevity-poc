package com.promevity.medai.service;

import com.promevity.medai.domain.Symptom;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bayesian Risk Calculator — Layer 3 of the Personalized Medicine AI stack.
 *
 * <h2>PoC implementation</h2>
 * <p>This class implements a simplified, <em>hardcoded</em> Bayesian rule set
 * sufficient for demonstrating the pipeline architecture.  In production, replace
 * the rule table with a proper probabilistic graphical model (e.g., using
 * <a href="https://github.com/pgmpy/pgmpy">pgmpy</a> via a microservice,
 * or a WASM-compiled Bayes Net library).
 *
 * <h2>Current rule set (P(Disease | Evidence))</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  Evidence set                             │ Disease               │ P   │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │  {Tachykardia, Fatigue}                   │ Thyroid Dysfunction   │ 85% │
 * │  {Tachykardia} only                       │ Thyroid Dysfunction   │ 40% │
 * │  {Fatigue} only                           │ Thyroid Dysfunction   │ 25% │
 * │  {} (no symptoms)                         │ Thyroid Dysfunction   │ 15% │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>How this maps to real Bayes Nets</h2>
 * <p>A full implementation would define a Directed Acyclic Graph (DAG) where:
 * <ul>
 *   <li>Parent nodes = latent disease variables (e.g. "Thyroid Dysfunction")</li>
 *   <li>Child nodes  = observable symptom variables ("Tachykardia", "Fatigue")</li>
 *   <li>CPTs (Conditional Probability Tables) encode P(symptom | disease)</li>
 * </ul>
 * Inference (Variable Elimination or Belief Propagation) computes
 * {@code P(disease | observed_symptoms)} using Bayes' theorem:
 * <pre>
 *   P(D|E) ∝ P(E|D) × P(D)
 * </pre>
 */
@ApplicationScoped
public class BayesianRiskService {

    // ── Disease identifiers ───────────────────────────────────────────────────
    private static final String THYROID_DYSFUNCTION = "Thyroid Dysfunction";

    // ── Prior probabilities (baseline prevalence in the general population) ──
    private static final double PRIOR_THYROID = 0.15; // 15 %

    // ── Conditional likelihoods P(symptom present | disease present) ─────────
    // These numbers are illustrative — a clinical expert should calibrate them.
    private static final double P_TACHYKARDIA_GIVEN_THYROID = 0.75;
    private static final double P_FATIGUE_GIVEN_THYROID     = 0.80;

    // ── Conditional likelihoods P(symptom present | disease absent) ──────────
    private static final double P_TACHYKARDIA_GIVEN_NO_THYROID = 0.25;
    private static final double P_FATIGUE_GIVEN_NO_THYROID     = 0.35;

    /**
     * Computes the posterior probability of a disease given the patient's
     * observed symptoms.
     *
     * <p>Algorithm: Naïve-Bayes approximation (assumes symptom independence
     * given the disease state — sufficient for a PoC).
     *
     * @param symptoms the full list of symptoms retrieved from the patient's
     *                 Knowledge Graph node
     * @return a {@link RiskAssessment} with the highest-probability disease
     */
    public RiskAssessment assess(List<Symptom> symptoms) {
        Set<String> symptomNames = symptoms.stream()
                .map(Symptom::name)
                .collect(Collectors.toSet());

        Log.debugf("Bayesian assessment for symptoms: %s", symptomNames);

        double posterior = computeThyroidPosterior(symptomNames);
        double probabilityPct = Math.round(posterior * 1000.0) / 10.0; // 1 decimal place

        List<String> evidenceList = List.copyOf(symptomNames);
        RiskAssessment result = RiskAssessment.of(THYROID_DYSFUNCTION, probabilityPct, evidenceList);

        Log.infof("Risk assessment result: %s @ %.1f%%", result.diseaseName(), result.probabilityPercentage());
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — Bayesian computation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Computes P(ThyroidDysfunction | observed symptoms) using a Naïve-Bayes
     * classifier with independent symptom assumption.
     *
     * <pre>
     *   P(D|E) = [P(D) × ∏ P(eᵢ|D)]  /  [P(D) × ∏ P(eᵢ|D) + P(¬D) × ∏ P(eᵢ|¬D)]
     * </pre>
     */
    private double computeThyroidPosterior(Set<String> observedSymptoms) {
        // P(D) and P(¬D)
        double pDisease   = PRIOR_THYROID;
        double pNoDisease = 1.0 - PRIOR_THYROID;

        // Likelihood accumulators
        double likelihoodGivenDisease   = 1.0;
        double likelihoodGivenNoDisease = 1.0;

        // ── Tachykardia factor ────────────────────────────────────────────────
        if (observedSymptoms.contains(Symptom.TACHYKARDIA)) {
            likelihoodGivenDisease   *= P_TACHYKARDIA_GIVEN_THYROID;
            likelihoodGivenNoDisease *= P_TACHYKARDIA_GIVEN_NO_THYROID;
        } else {
            // complement: P(symptom absent | disease)
            likelihoodGivenDisease   *= (1.0 - P_TACHYKARDIA_GIVEN_THYROID);
            likelihoodGivenNoDisease *= (1.0 - P_TACHYKARDIA_GIVEN_NO_THYROID);
        }

        // ── Fatigue factor ────────────────────────────────────────────────────
        if (observedSymptoms.contains(Symptom.FATIGUE)) {
            likelihoodGivenDisease   *= P_FATIGUE_GIVEN_THYROID;
            likelihoodGivenNoDisease *= P_FATIGUE_GIVEN_NO_THYROID;
        } else {
            likelihoodGivenDisease   *= (1.0 - P_FATIGUE_GIVEN_THYROID);
            likelihoodGivenNoDisease *= (1.0 - P_FATIGUE_GIVEN_NO_THYROID);
        }

        // ── Bayes' theorem — unnormalised posteriors ──────────────────────────
        double numerator   = pDisease   * likelihoodGivenDisease;
        double denominator = numerator  + (pNoDisease * likelihoodGivenNoDisease);

        // Guard against division by zero (degenerate likelihoods)
        if (denominator == 0.0) {
            Log.warn("Bayesian denominator is zero — returning prior probability");
            return pDisease;
        }

        return numerator / denominator;
    }
}
