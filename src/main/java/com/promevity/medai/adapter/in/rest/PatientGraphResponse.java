package com.promevity.medai.adapter.in.rest;

import com.promevity.medai.application.port.in.PatientGraphResult;

import java.util.List;

/**
 * REST-DTO — Antwort des {@code GET /api/diagnose/patient/{patientId}/graph}-Endpunkts.
 *
 * <p>Enthält alle Knoten und Kanten, die Cytoscape.js im Browser benötigt, um
 * den Patient-Digital-Twin-Graphen zu rendern.
 *
 * @param patientNode  der Patient-Knoten
 * @param symptomNodes alle Symptom-Knoten des Patienten
 * @param edges        EXPERIENCES-Kanten Patient → Symptom (inkl. letztem Datum)
 */
public record PatientGraphResponse(
        GraphNode patientNode,
        List<GraphNode> symptomNodes,
        List<GraphEdge> edges
) {

    public record GraphNode(String id, String label, String type) {}

    public record GraphEdge(String from, String to, String date) {}

    static PatientGraphResponse from(PatientGraphResult result) {
        var patientNode = new GraphNode(
                result.patientId(),
                result.patientName() + " (" + result.patientAge() + " J.)",
                "PATIENT");

        var symptomNodes = result.symptoms().stream()
                .map(s -> new GraphNode(s.id(), s.name(), "SYMPTOM"))
                .toList();

        var edges = result.symptoms().stream()
                .map(s -> new GraphEdge(result.patientId(), s.id(), s.latestDate()))
                .toList();

        return new PatientGraphResponse(patientNode, symptomNodes, edges);
    }
}
