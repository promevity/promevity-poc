package com.promevity.medai.adapter.out.llm;

import com.promevity.medai.application.port.out.MedicalExplainerPort;
import com.promevity.medai.domain.model.RiskAssessment;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Template-basierter Explainer — Standard-Implementierung von {@link MedicalExplainerPort}.
 *
 * <p>Generiert deterministisch eine patientengerechte, empathische Erklärung
 * auf Deutsch direkt aus dem {@link RiskAssessment}-Domänenobjekt.
 * Kein externer LLM-Aufruf, kein API-Key, keine Rate-Limits, keine Latenz.
 *
 * <p><b>Aktivierung:</b> Diese Klasse ist der CDI-Default und wird automatisch
 * injiziert, solange {@code LlmMedicalExplainer} nicht als Alternative aktiviert ist.
 *
 * <p><b>LLM opt-in:</b> Um Ollama zu nutzen, folgende Zeile in {@code application.properties}
 * einkommentieren:
 * <pre>
 *   quarkus.arc.selected-alternatives=com.promevity.medai.adapter.out.llm.LlmMedicalExplainer
 * </pre>
 */
@ApplicationScoped
public class TemplateMedicalExplainer implements MedicalExplainerPort {

    @Override
    public String explain(String patientName, RiskAssessment assessment,
                          String rawText, String vitalSummary) {
        return explain(patientName, assessment, vitalSummary);
    }

    /** Interne Überladung — wird auch von {@link LlmMedicalExplainer} als Fallback genutzt. */
    String explain(String patientName, RiskAssessment assessment, String vitalSummary) {
        double pct        = assessment.probabilityPercentage();
        String disease    = assessment.diseaseName();
        List<String> syms = assessment.evidenceSymptoms();

        String symptomsText = syms.isEmpty()
                ? "allgemeine Beschwerden"
                : String.join(", ", syms);

        String urgency;
        String action;
        if (pct >= 65) {
            urgency = "Das analysierte Muster ergibt ein deutliches Signal: die Wahrscheinlichkeit "
                    + "von " + fmt(pct) + " % deutet darauf hin, dass dieser Befund zeitnah "
                    + "abgeklärt werden sollte.";
            action = "Bitte vereinbaren Sie möglichst bald einen Termin bei Ihrem Hausarzt, "
                    + "um die Befunde gemeinsam zu besprechen.";
        } else if (pct >= 35) {
            urgency = "Die Analyse zeigt ein moderates Signal (" + fmt(pct) + " % "
                    + "Wahrscheinlichkeit) — ein Hinweis, den es wert ist, ärztlich zu besprechen.";
            action = "Es empfiehlt sich, beim nächsten Arzttermin über diese Symptome zu sprechen.";
        } else {
            urgency = "Das analysierte Muster ist noch schwach (" + fmt(pct) + " % "
                    + "Wahrscheinlichkeit). Es können viele Ursachen hinter diesen Beschwerden "
                    + "stecken.";
            action = "Beobachten Sie Ihre Symptome weiter und sprechen Sie bei anhaltenden "
                    + "Beschwerden mit Ihrem Arzt.";
        }

        String vitalsNote = (vitalSummary != null && !vitalSummary.isBlank()
                && !vitalSummary.equals("—"))
                ? " Ihre Wearable-Messung ergab folgende Werte: " + vitalSummary + "."
                : "";

        return "Guten Tag, " + patientName + "! Vielen Dank, dass Sie Ihre Beschwerden mitgeteilt "
                + "haben. Die berichteten Symptome (" + symptomsText + ") wurden sorgfältig analysiert.\n\n"
                + urgency + " Auf Basis Ihrer Angaben ergibt die computergestützte Risikobewertung "
                + "einen Hinweis auf " + disease + "." + vitalsNote + " Bitte beachten Sie: Dies ist "
                + "keine ärztliche Diagnose, sondern eine statistische Einschätzung, die als "
                + "Gesprächsgrundlage für Ihren Arzt dienen soll.\n\n"
                + action + " Nur ein qualifizierter Arzt kann eine verlässliche Diagnose stellen — "
                + "bitte zögern Sie nicht, professionellen Rat einzuholen.";
    }

    private static String fmt(double pct) {
        return String.format("%.0f", pct);
    }
}
