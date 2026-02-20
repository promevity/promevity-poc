package com.promevity.medai.application.port.in;

import com.promevity.medai.domain.model.Patient;

/**
 * Primärer Port (Driving Port) — "Patienten registrieren".
 *
 * <p><b>Hexagonale Architektur — Application Layer / Port IN</b><br>
 * Ermöglicht dem REST-Adapter, einen neuen Patienten im Knowledge Graph
 * anzulegen, ohne direkt mit der Datenbankschicht zu interagieren.
 *
 * <p>Implementiert von: {@code DiagnosticService}
 * <br>Aufgerufen von:   {@code DiagnosticResource} (REST-Adapter)
 */
public interface RegisterPatientUseCase {

    /**
     * Legt einen Patienten idempotent im Knowledge Graph an (MERGE-Semantik).
     *
     * @param patient der zu registrierende Patient
     * @return der gespeicherte Patient (unveränderter Pass-through für den PoC)
     */
    Patient register(Patient patient);
}
