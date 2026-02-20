package com.promevity.medai.llm;

import com.promevity.medai.service.RiskAssessment;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * LLM Orchestrator — Layer 4 of the Personalized Medicine AI stack.
 *
 * <p>This interface is the <em>communicator</em> between the structured
 * probabilistic output of the Bayesian engine and the end user.  LangChain4j
 * (via the {@code quarkus-langchain4j-openai} extension) generates a CDI bean
 * at build time that proxies every method call to the configured LLM.
 *
 * <h2>Responsibilities</h2>
 * <ol>
 *   <li>Receive the {@link RiskAssessment} produced by
 *       {@link com.promevity.medai.service.BayesianRiskService}.</li>
 *   <li>Receive the raw patient text for additional conversational context.</li>
 *   <li>Generate a patient-friendly, empathetic explanation that:
 *       <ul>
 *         <li>Summarises the detected symptoms.</li>
 *         <li>Explains the probabilistic risk in lay terms.</li>
 *         <li>Recommends a next step (e.g., see a doctor) without diagnosing.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <h2>Why @RegisterAiService?</h2>
 * <p>The {@code @RegisterAiService} annotation is the Quarkus-LangChain4j
 * equivalent of {@code @FeignClient} or Spring's {@code @Service} — it wires
 * the interface to the {@code ChatLanguageModel} bean configured via
 * {@code application.properties} (model name, temperature, timeout, …).
 *
 * <h2>Prompt engineering notes</h2>
 * <ul>
 *   <li>The {@code @SystemMessage} establishes the assistant persona and hard
 *       constraints (no diagnosis, empathy, brevity).</li>
 *   <li>The {@code @UserMessage} is a Mustache template; LangChain4j replaces
 *       {@code {{param}}} placeholders with the method argument values at
 *       runtime.</li>
 *   <li>Keeping the risk score and symptoms in the user turn (not the system
 *       turn) allows per-call variation without model fine-tuning.</li>
 * </ul>
 */
@RegisterAiService
public interface MedicalAssistant {

    /**
     * Generates a patient-friendly health summary from the Bayesian risk result.
     *
     * @param patientName    the patient's first name for personalisation
     * @param symptoms       comma-separated list of detected symptom names
     * @param diseaseName    the disease name returned by the Bayesian engine
     * @param probabilityPct the posterior probability as a percentage (e.g. 85.0)
     * @param rawText        the original free-text the patient submitted
     * @return a concise, empathetic, plain-language health summary (≤ 200 words)
     */
    @SystemMessage("""
            You are a compassionate and knowledgeable medical communication assistant.
            Your role is to translate complex medical risk assessments into clear,
            empathetic language that a non-medical patient can understand.

            STRICT GUIDELINES:
            - You are NOT a doctor and NEVER provide a medical diagnosis.
            - Always recommend consulting a qualified healthcare professional.
            - Use warm, reassuring, non-alarmist language.
            - Keep your response under 200 words.
            - Structure your response in three short paragraphs:
              1. Acknowledge the reported symptoms with empathy.
              2. Explain the risk assessment result in simple terms, using the
                 probability as an indicator of "how worth investigating" rather
                 than a certainty.
              3. Recommend a clear next step (e.g., schedule a GP appointment).
            - Do not repeat the patient's name more than once.
            - Do not use medical jargon without a plain-English explanation.
            """)
    @UserMessage("""
            Patient name:       {{patientName}}
            Detected symptoms:  {{symptoms}}
            Risk assessment:    {{diseaseName}} — {{probabilityPct}}% probability
            Patient's own words: "{{rawText}}"

            Please generate the patient-friendly health summary now.
            """)
    String explain(
            String patientName,
            String symptoms,
            String diseaseName,
            double probabilityPct,
            String rawText
    );
}
