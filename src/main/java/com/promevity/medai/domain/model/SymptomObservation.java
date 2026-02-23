package com.promevity.medai.domain.model;

/**
 * Value Object — eine einzelne EXPERIENCES-Beziehung aus dem Neo4j Knowledge Graph.
 *
 * <p>Im Gegensatz zu {@link SymptomHistoryEntry} (nur letztes Datum) enthält
 * dieses Objekt genau einen Beobachtungszeitpunkt, damit die Symptom-Timeline
 * alle historischen Einträge chronologisch auflisten kann.
 *
 * @param symptom      das beobachtete Symptom
 * @param observedDate Beobachtungsdatum (ISO-8601-String, z. B. "2025-01-15")
 */
public record SymptomObservation(Symptom symptom, String observedDate) {}
