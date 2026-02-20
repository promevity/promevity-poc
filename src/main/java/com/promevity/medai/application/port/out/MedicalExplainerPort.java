package com.promevity.medai.application.port.out;

import com.promevity.medai.domain.model.RiskAssessment;

/**
 * Sekundärer Port (Driven Port) — patientengerechte Erklärung per LLM.
 *
 * <p><b>Hexagonale Architektur — Application Layer / Port OUT</b><br>
 * Der Anwendungskern ruft dieses Interface auf, um aus dem Domänenobjekt
 * {@link RiskAssessment} eine natürlichsprachliche Erklärung zu erzeugen.
 * Er weiß dabei <em>nicht</em>, ob hinter dem Port GPT-4o-mini, ein
 * lokales Ollama-Modell oder ein Mock steckt — das ist ausschließlich
 * Sache des Adapters.
 *
 * <p>Implementiert von: {@code LlmMedicalExplainer} (Adapter/Out/LLM)
 * <br>Aufgerufen von:   {@code DiagnosticService}    (Application/Service)
 */
public interface MedicalExplainerPort {

    /**
     * Erzeugt eine empathische, verständliche Erklärung für den Patienten.
     *
     * @param patientName    Vorname des Patienten (für Personalisierung)
     * @param assessment     Ergebnis des Bayesianischen Netzes
     * @param rawText        ursprünglicher Text des Patienten (Kontext für LLM)
     * @return Freitext-Erklärung (≤ 200 Wörter)
     */
    String explain(String patientName, RiskAssessment assessment, String rawText);
}
