package com.promevity.medai.adapter.out.llm;

import com.promevity.medai.application.port.out.MedicalExplainerPort;
import com.promevity.medai.domain.model.RiskAssessment;
import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

/**
 * LLM-basierter Explainer — <em>optionale</em> Alternative zu {@link TemplateMedicalExplainer}.
 *
 * <p><b>Standardmäßig inaktiv.</b> Um diesen Explainer zu aktivieren, folgende Zeile
 * in {@code application.properties} einkommentieren:
 * <pre>
 *   quarkus.arc.selected-alternatives=com.promevity.medai.adapter.out.llm.LlmMedicalExplainer
 * </pre>
 *
 * <p>Unterstützte LLM-Provider (in {@code application.properties} konfigurierbar):
 * <ul>
 *   <li>Ollama (lokal, kein API-Key) — empfohlen für schnelle Hardware (≥ 2020)</li>
 *   <li>Google AI Gemini — kostenlos, aber Rate-Limits auf Free Tier</li>
 * </ul>
 *
 * <p>Ist das LLM nicht erreichbar oder antwortet es nicht rechtzeitig, greift
 * automatisch der {@link TemplateMedicalExplainer} als Fallback.
 */
@Alternative
@Priority(10)
@ApplicationScoped
public class LlmMedicalExplainer implements MedicalExplainerPort {

    @Inject
    MedicalAssistant medicalAssistant;

    @Inject
    TemplateMedicalExplainer templateExplainer;

    @Override
    public String explain(String patientName, RiskAssessment assessment,
                          String rawText, String vitalSummary) {
        String symptomsJoined = String.join(", ", assessment.evidenceSymptoms());

        Log.debugf("[LLM] Erklärungs-Request: patient=%s, disease=%s, p=%.1f%%",
                patientName, assessment.diseaseName(), assessment.probabilityPercentage());

        try {
            String result = medicalAssistant.explain(
                    patientName,
                    symptomsJoined,
                    assessment.diseaseName(),
                    assessment.probabilityPercentage(),
                    rawText,
                    vitalSummary
            );
            Log.debugf("[LLM] Antwort erhalten (%d Zeichen)", result.length());
            return result;

        } catch (Exception e) {
            Log.warnf("[LLM] LLM nicht verfügbar (%s) — Template-Fallback aktiv", e.getMessage());
            return templateExplainer.explain(patientName, assessment, vitalSummary);
        }
    }
}
