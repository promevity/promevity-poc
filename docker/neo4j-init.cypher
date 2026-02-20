// ═══════════════════════════════════════════════════════════════════════════
//  Neo4j — Initial Seed Data for Personalized Medicine AI PoC
//
//  Run this script once against a fresh Neo4j instance to create the Medical
//  Ontology (Symptom nodes) and a sample Patient with known symptoms.
//
//  Execute via:
//    docker exec -i <neo4j-container> cypher-shell \
//      -u neo4j -p medai-secret < docker/neo4j-init.cypher
//
//  Or paste it into the Neo4j Browser at http://localhost:7474
// ═══════════════════════════════════════════════════════════════════════════

// ── 1. Constraints & indexes (idempotent) ────────────────────────────────
CREATE CONSTRAINT patient_id_unique IF NOT EXISTS
    FOR (p:Patient) REQUIRE p.id IS UNIQUE;

CREATE CONSTRAINT symptom_id_unique IF NOT EXISTS
    FOR (s:Symptom) REQUIRE s.id IS UNIQUE;

CREATE INDEX symptom_name_index IF NOT EXISTS
    FOR (s:Symptom) ON (s.name);

// ── 2. Medical Ontology — Symptom nodes ──────────────────────────────────
//    These nodes represent the controlled vocabulary / ontology layer.
//    In production, extend with externalCode (SNOMED-CT / ICD-11 concept IDs).

MERGE (tachykardia:Symptom {id: 'symptom-tachykardia'})
ON CREATE SET tachykardia.name = 'Tachykardia';

MERGE (fatigue:Symptom {id: 'symptom-fatigue'})
ON CREATE SET fatigue.name = 'Fatigue';

MERGE (tremor:Symptom {id: 'symptom-tremor'})
ON CREATE SET tremor.name = 'Tremor';

MERGE (weightLoss:Symptom {id: 'symptom-weight-loss'})
ON CREATE SET weightLoss.name = 'Weight Loss';

MERGE (heatIntolerance:Symptom {id: 'symptom-heat-intolerance'})
ON CREATE SET heatIntolerance.name = 'Heat Intolerance';

MERGE (anxiety:Symptom {id: 'symptom-anxiety'})
ON CREATE SET anxiety.name = 'Anxiety';

// ── 3. Sample Patients ────────────────────────────────────────────────────

MERGE (alice:Patient {id: 'patient-001'})
ON CREATE SET alice.name = 'Alice Müller', alice.age = 42;

MERGE (bob:Patient {id: 'patient-002'})
ON CREATE SET bob.name = 'Bob Schmidt', bob.age = 67;

// ── 4. Patient Digital Twin — EXPERIENCES relationships ─────────────────
//    (Patient)-[:EXPERIENCES {date}]->(Symptom)
//    Dates simulate a timeline of symptom observations.

// Alice — already has both key risk symptoms (high-risk scenario)
MATCH (alice:Patient {id: 'patient-001'}), (t:Symptom {id: 'symptom-tachykardia'})
MERGE (alice)-[:EXPERIENCES {date: '2025-01-15'}]->(t);

MATCH (alice:Patient {id: 'patient-001'}), (f:Symptom {id: 'symptom-fatigue'})
MERGE (alice)-[:EXPERIENCES {date: '2025-01-15'}]->(f);

// Bob — only fatigue so far (low-risk scenario)
MATCH (bob:Patient {id: 'patient-002'}), (f:Symptom {id: 'symptom-fatigue'})
MERGE (bob)-[:EXPERIENCES {date: '2025-02-01'}]->(f);

// ── 5. Verification queries (comment out when running as an init script) ─
// MATCH (p:Patient)-[r:EXPERIENCES]->(s:Symptom)
// RETURN p.name AS patient, s.name AS symptom, r.date AS observedOn
// ORDER BY patient, observedOn;
