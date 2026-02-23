package com.promevity.medai.adapter.out.llm;

import com.promevity.medai.application.port.out.MedicalExplainerPort;
import com.promevity.medai.domain.model.RiskAssessment;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.StringJoiner;

/**
 * Getriebener Adapter — LLM-Implementierung von {@link MedicalExplainerPort}.
 *
 * <p><b>Hexagonale Architektur — Adapter OUT / LLM</b><br>
 * Diese Klasse ist der Übersetzungsschicht zwischen dem technologieagnostischen
 * Port der Applikationsschicht und der konkreten LangChain4j-Implementierung.
 *
 * <h2>Verantwortlichkeiten</h2>
 * <ol>
 *   <li>Empfängt den domänenspezifischen {@link RiskAssessment} vom Use Case.</li>
 *   <li>Transformiert ihn in primitive Parameter für das LangChain4j-Interface.</li>
 *   <li>Delegiert den eigentlichen LLM-Aufruf an {@link MedicalAssistant}.</li>
 *   <li>Gibt die generierte Erklärung zurück.</li>
 * </ol>
 *
 * <p>Durch diese Trennung kann {@link MedicalAssistant} (framework-spezifisch)
 * package-private bleiben und ist von außen nicht direkt injizierbar.
 *
 * <h2>Implementierter Port</h2>
 * {@link MedicalExplainerPort}
 */
@ApplicationScoped
public class LlmMedicalExplainer implements MedicalExplainerPort {

    /** Package-privates LangChain4j-Interface — nur innerhalb dieses Adapters sichtbar. */
    @Inject
    MedicalAssistant medicalAssistant;

    @Override
    public String explain(String patientName, RiskAssessment assessment, String rawText, String vitalSummary) {
        // Domänenobjekt → primitive LLM-Parameter aufbereiten
        String symptomsJoined = String.join(", ", assessment.evidenceSymptoms());

        Log.debugf("[LLM] Erklärungs-Request: patient=%s, disease=%s, p=%.1f%%",
                patientName, assessment.diseaseName(), assessment.probabilityPercentage());

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
    }
}
