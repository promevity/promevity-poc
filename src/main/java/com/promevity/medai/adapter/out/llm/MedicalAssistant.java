package com.promevity.medai.adapter.out.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * LangChain4j-KI-Service — rohe LLM-Schnittstelle.
 *
 * <p><b>Hexagonale Architektur — Adapter OUT / LLM</b><br>
 * Dieses Interface ist <em>ausschließlich</em> für den LLM-Adapter sichtbar.
 * Der Anwendungskern kennt nur den {@code MedicalExplainerPort} — nicht dieses
 * Framework-spezifische Interface.
 *
 * <p>LangChain4j generiert zur Build-Zeit einen CDI-Proxy, der jeden
 * Methodenaufruf an den konfigurierten LLM-Provider weiterleitet
 * (aktuell: OpenAI GPT-4o-mini, konfigurierbar via {@code application.properties}).
 *
 * <p>Prompt-Engineering-Hinweise:
 * <ul>
 *   <li>{@code @SystemMessage} — Persona + harte Constraints (kein Diagnosestellen,
 *       Empathie, max. 200 Wörter).</li>
 *   <li>{@code @UserMessage} — Mustache-Template; LangChain4j ersetzt
 *       {@code {{param}}}-Platzhalter zur Laufzeit durch die Methodenargumente.</li>
 * </ul>
 */
@RegisterAiService
interface MedicalAssistant {

    @SystemMessage("""
            Du bist ein einfühlsamer und kompetenter medizinischer Kommunikationsassistent.
            Deine Aufgabe ist es, komplexe medizinische Risikobewertungen in klare,
            empathische Sprache zu übersetzen, die ein medizinischer Laie versteht.

            STRIKTE REGELN:
            - Du bist KEIN Arzt und stellst NIEMALS eine Diagnose.
            - Empfehle immer, einen qualifizierten Arzt aufzusuchen.
            - Verwende warme, beruhigende, nicht alarmistische Sprache.
            - Halte deine Antwort unter 200 Wörtern.
            - Strukturiere die Antwort in drei kurze Absätze:
              1. Empathische Anerkennung der berichteten Beschwerden.
              2. Einfache Erklärung des Risikobewertungsergebnisses (Wahrscheinlichkeit
                 als Maß für "wie wichtig ist es, dies abzuklären", nicht als Gewissheit).
              3. Klare Handlungsempfehlung (z. B. Termin beim Hausarzt vereinbaren).
            - Nenne den Patientennamen maximal einmal.
            - Benutze keinen medizinischen Fachbegriff ohne Erklärung in Klammern.
            """)
    @UserMessage("""
            Patientenname:      {{patientName}}
            Erkannte Symptome:  {{symptoms}}
            Risikobewertung:    {{diseaseName}} — {{probabilityPct}} % Wahrscheinlichkeit
            Eigene Beschreibung: "{{rawText}}"

            Bitte erstelle jetzt die patientengerechte Gesundheitszusammenfassung.
            """)
    String explain(
            String patientName,
            String symptoms,
            String diseaseName,
            double probabilityPct,
            String rawText
    );
}
