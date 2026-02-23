package com.promevity.medai.application.port.out;

import com.promevity.medai.domain.model.DiseaseCptData;

import java.util.List;

/**
 * Sekundärer Port — Zugriff auf die medizinische Wissensbasis (CPT-Daten).
 *
 * <p><b>Hexagonale Architektur — Application Layer / Port OUT</b><br>
 * Dieser Port abstrahiert den Zugriff auf die im Knowledge Graph gespeicherten
 * Conditional Probability Tables (CPTs).  Der Anwendungskern kennt nur dieses
 * Interface — nicht die konkrete Neo4j-Implementierung dahinter.
 *
 * <p>Implementiert von:
 * {@code Neo4jDiseaseKnowledgeRepository} (Adapter OUT / Neo4j)
 */
public interface DiseaseKnowledgePort {

    /**
     * Lädt alle Erkrankungen mit ihren CPT-Daten aus dem Knowledge Graph.
     *
     * @return unveränderliche Liste aller {@link DiseaseCptData}-Objekte;
     *         im Fehlerfall wird eine leere Liste zurückgegeben.
     */
    List<DiseaseCptData> loadAll();
}
