package com.promevity.medai.adapter.out.llm;

import com.promevity.medai.application.port.out.MedicalExplainerPort;
import com.promevity.medai.domain.model.RiskAssessment;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Getriebener Adapter — LLM-Implementierung von {@link MedicalExplainerPort}.
 *
 * <p><b>Primäres LLM:</b> Ollama (lokal, kein API-Key, keine Rate-Limits).<br>
 * <b>Fallback:</b> {@link TemplateMedicalExplainer} — generiert deterministisch
 * eine empathische Erklärung ohne externe Abhängigkeiten.
 *
 * <p>Der Fallback greift automatisch, wenn Ollama nicht erreichbar ist
 * (z. B. Modell noch nicht heruntergeladen, Service startet noch).
 * Die Demo ist damit immer funktionsfähig.
 */
@ApplicationScoped
public class LlmMedicalExplainer implements MedicalExplainerPort {

    @Inject
    MedicalAssistant medicalAssistant;

    @Inject
    TemplateMedicalExplainer templateExplainer;

    @Override
    public String explain(String patientName, RiskAssessment assessment, String rawText, String vitalSummary) {
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
            Log.debugf("[LLM] Ollama-Antwort erhalten (%d Zeichen)", result.length());
            return result;

        } catch (Exception e) {
            Log.warnf("[LLM] Ollama nicht verfügbar (%s) — Template-Fallback aktiv", e.getMessage());
            return templateExplainer.explain(patientName, assessment, vitalSummary);
        }
    }
}

