package com.promevity.medai.domain.model;

/**
 * Value Object — ein Symptom-Eintrag aus der Patientenhistorie mit dem jüngsten Beobachtungsdatum.
 *
 * <p>Wird von {@code PatientRepositoryPort#findSymptomHistory} zurückgegeben
 * und enthält neben dem Symptom-Knoten das letzte {@code EXPERIENCES}-Datum
 * aus dem Knowledge Graph.
 *
 * @param symptom    das Symptom-Domänenobjekt
 * @param latestDate letztes Beobachtungsdatum (ISO-8601-String, z. B. "2026-02-23")
 */
public record SymptomHistoryEntry(Symptom symptom, String latestDate) {}
