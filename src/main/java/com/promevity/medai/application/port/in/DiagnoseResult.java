package com.promevity.medai.application.port.in;

import com.promevity.medai.domain.model.DifferentialDiagnosis;
import com.promevity.medai.domain.model.SymptomSource;
import com.promevity.medai.domain.model.VitalReading;

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
 * @param differentialDiagnoses alle Erkrankungen absteigend nach Wahrscheinlichkeit sortiert
 * @param explanation           patientengerechte LLM-Erklärung
 * @param vitalSummary          Zusammenfassung der Garmin-Vitaldaten (leer wenn keine vorhanden)
 * @param symptomSources        Herkunft jedes Symptoms (TEXT / GARMIN / VERLAUF)
 * @param vitals                strukturierte Vitaldaten für das Frontend-Dashboard
 */
public record DiagnoseResult(
        String patientId,
        List<String> allSymptoms,
        String diseaseName,
        double probabilityPercentage,
        List<DifferentialDiagnosis> differentialDiagnoses,
        String explanation,
        String vitalSummary,
        List<SymptomSource> symptomSources,
        List<VitalReading> vitals
) {}
