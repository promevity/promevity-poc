package com.promevity.medai.application.port.in;

import java.util.List;

/**
 * Ausgabe-DTO des primären Ports {@link DiagnoseUseCase}.
 *
 * <p><b>Hexagonale Architektur — Application Layer</b><br>
 * Dieses Record-Objekt überträgt das Ergebnis des Use Case vom Anwendungskern
 * zum treibenden Adapter.  Der REST-Adapter mappt es anschließend auf seine
 * eigene {@code DiagnoseResponse} (mit Jackson-Annotationen etc.), ohne dass
 * die Applikationsschicht von HTTP-Belangen abhängt.
 *
 * @param patientId             ID des Patienten
 * @param allSymptoms           alle bekannten Symptome aus dem Knowledge Graph
 * @param diseaseName           Krankheit mit höchster Posteriori-Wahrscheinlichkeit
 * @param probabilityPercentage Wahrscheinlichkeit in Prozent
 * @param explanation           patientengerechte LLM-Erklärung
 */
public record DiagnoseResult(
        String patientId,
        List<String> allSymptoms,
        String diseaseName,
        double probabilityPercentage,
        String explanation
) {}
