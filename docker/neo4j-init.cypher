// ═══════════════════════════════════════════════════════════════════════════
//  Neo4j — Initial Seed Data for Personalized Medicine AI PoC
//
//  Run this script once against a fresh Neo4j instance to create the Medical
//  Ontology (Disease + Symptom nodes + CPT edges) and sample Patients with
//  known symptoms.
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

CREATE CONSTRAINT disease_id_unique IF NOT EXISTS
    FOR (d:Disease) REQUIRE d.id IS UNIQUE;

CREATE INDEX symptom_name_index IF NOT EXISTS
    FOR (s:Symptom) ON (s.name);

CREATE INDEX disease_name_index IF NOT EXISTS
    FOR (d:Disease) ON (d.name);

// ── 2. Medical Ontology — Disease nodes with Bayesian priors ─────────────
//    P(D) = population-level base rate for this PoC.
//    In production: derive from epidemiological databases (GBD, WHO, ICD-11).

MERGE (thyroid:Disease {id: 'disease-thyroid-dysfunction'})
ON CREATE SET thyroid.name = 'Thyroid Dysfunction', thyroid.prior = 0.15;

MERGE (anemia:Disease {id: 'disease-anemia'})
ON CREATE SET anemia.name = 'Anemia', anemia.prior = 0.12;

MERGE (arrhythmia:Disease {id: 'disease-cardiac-arrhythmia'})
ON CREATE SET arrhythmia.name = 'Cardiac Arrhythmia', arrhythmia.prior = 0.08;

// ── 3. Medical Ontology — Symptom nodes ──────────────────────────────────
//    These nodes form the controlled vocabulary / ontology layer.
//    In production: annotate with externalCode (SNOMED-CT / ICD-11 concept IDs).

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

MERGE (pallor:Symptom {id: 'symptom-pallor'})
ON CREATE SET pallor.name = 'Pallor';

MERGE (breathlessness:Symptom {id: 'symptom-breathlessness'})
ON CREATE SET breathlessness.name = 'Breathlessness';

MERGE (weakness:Symptom {id: 'symptom-weakness'})
ON CREATE SET weakness.name = 'Weakness';

MERGE (chestPain:Symptom {id: 'symptom-chest-pain'})
ON CREATE SET chestPain.name = 'Chest Pain';

// ── 4. Medical Ontology — CPT edges (Conditional Probability Tables) ─────
//    (:Disease)-[:HAS_SYMPTOM_LIKELIHOOD {
//        p_given_disease:    P(symptom | disease),
//        p_given_no_disease: P(symptom | ¬disease)
//    }]->(:Symptom)
//
//    These probabilities encode the Naïve Bayes CPT and serve as the
//    knowledge source for the graph-driven BayesianRiskService.
//    Source: calibrated for PoC demo; in production use literature values.

// — Thyroid Dysfunction —
MATCH (d:Disease {id: 'disease-thyroid-dysfunction'}), (s:Symptom {id: 'symptom-tachykardia'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.75, r.p_given_no_disease = 0.20;

MATCH (d:Disease {id: 'disease-thyroid-dysfunction'}), (s:Symptom {id: 'symptom-fatigue'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.70, r.p_given_no_disease = 0.40;

MATCH (d:Disease {id: 'disease-thyroid-dysfunction'}), (s:Symptom {id: 'symptom-tremor'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.50, r.p_given_no_disease = 0.08;

MATCH (d:Disease {id: 'disease-thyroid-dysfunction'}), (s:Symptom {id: 'symptom-weight-loss'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.65, r.p_given_no_disease = 0.10;

MATCH (d:Disease {id: 'disease-thyroid-dysfunction'}), (s:Symptom {id: 'symptom-heat-intolerance'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.70, r.p_given_no_disease = 0.05;

MATCH (d:Disease {id: 'disease-thyroid-dysfunction'}), (s:Symptom {id: 'symptom-anxiety'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.55, r.p_given_no_disease = 0.25;

// — Anemia —
MATCH (d:Disease {id: 'disease-anemia'}), (s:Symptom {id: 'symptom-fatigue'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.90, r.p_given_no_disease = 0.40;

MATCH (d:Disease {id: 'disease-anemia'}), (s:Symptom {id: 'symptom-tachykardia'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.55, r.p_given_no_disease = 0.20;

MATCH (d:Disease {id: 'disease-anemia'}), (s:Symptom {id: 'symptom-pallor'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.75, r.p_given_no_disease = 0.05;

MATCH (d:Disease {id: 'disease-anemia'}), (s:Symptom {id: 'symptom-breathlessness'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.65, r.p_given_no_disease = 0.15;

MATCH (d:Disease {id: 'disease-anemia'}), (s:Symptom {id: 'symptom-weakness'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.80, r.p_given_no_disease = 0.30;

// — Cardiac Arrhythmia —
MATCH (d:Disease {id: 'disease-cardiac-arrhythmia'}), (s:Symptom {id: 'symptom-tachykardia'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.90, r.p_given_no_disease = 0.20;

MATCH (d:Disease {id: 'disease-cardiac-arrhythmia'}), (s:Symptom {id: 'symptom-chest-pain'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.65, r.p_given_no_disease = 0.10;

MATCH (d:Disease {id: 'disease-cardiac-arrhythmia'}), (s:Symptom {id: 'symptom-breathlessness'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.60, r.p_given_no_disease = 0.15;

MATCH (d:Disease {id: 'disease-cardiac-arrhythmia'}), (s:Symptom {id: 'symptom-fatigue'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.50, r.p_given_no_disease = 0.40;

MATCH (d:Disease {id: 'disease-cardiac-arrhythmia'}), (s:Symptom {id: 'symptom-anxiety'})
MERGE (d)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s)
ON CREATE SET r.p_given_disease = 0.50, r.p_given_no_disease = 0.25;

// ── 5. Sample Patients ────────────────────────────────────────────────────

MERGE (alice:Patient {id: 'patient-001'})
ON CREATE SET alice.name = 'Alice Müller', alice.age = 42;

MERGE (bob:Patient {id: 'patient-002'})
ON CREATE SET bob.name = 'Bob Schmidt', bob.age = 67;

MERGE (charlie:Patient {id: 'patient-003'})
ON CREATE SET charlie.name = 'Charlie Bauer', charlie.age = 55;

// ── 6. Patient Digital Twin — EXPERIENCES relationships ─────────────────
//    (Patient)-[:EXPERIENCES {date}]->(Symptom)
//    Dates simulate a timeline of symptom observations.

// Alice — Tachykardia + Fatigue + Weight Loss → strong Thyroid Dysfunction signal
MATCH (alice:Patient {id: 'patient-001'}), (t:Symptom {id: 'symptom-tachykardia'})
MERGE (alice)-[:EXPERIENCES {date: '2025-01-15'}]->(t);

MATCH (alice:Patient {id: 'patient-001'}), (f:Symptom {id: 'symptom-fatigue'})
MERGE (alice)-[:EXPERIENCES {date: '2025-01-15'}]->(f);

MATCH (alice:Patient {id: 'patient-001'}), (w:Symptom {id: 'symptom-weight-loss'})
MERGE (alice)-[:EXPERIENCES {date: '2025-01-20'}]->(w);

// Bob — Fatigue + Pallor + Weakness → Anemia vs Thyroid competition
MATCH (bob:Patient {id: 'patient-002'}), (f:Symptom {id: 'symptom-fatigue'})
MERGE (bob)-[:EXPERIENCES {date: '2025-02-01'}]->(f);

MATCH (bob:Patient {id: 'patient-002'}), (p:Symptom {id: 'symptom-pallor'})
MERGE (bob)-[:EXPERIENCES {date: '2025-02-01'}]->(p);

MATCH (bob:Patient {id: 'patient-002'}), (w:Symptom {id: 'symptom-weakness'})
MERGE (bob)-[:EXPERIENCES {date: '2025-02-05'}]->(w);

// Charlie — Tachykardia + Chest Pain + Breathlessness → Cardiac Arrhythmia signal
MATCH (charlie:Patient {id: 'patient-003'}), (t:Symptom {id: 'symptom-tachykardia'})
MERGE (charlie)-[:EXPERIENCES {date: '2025-03-01'}]->(t);

MATCH (charlie:Patient {id: 'patient-003'}), (c:Symptom {id: 'symptom-chest-pain'})
MERGE (charlie)-[:EXPERIENCES {date: '2025-03-01'}]->(c);

MATCH (charlie:Patient {id: 'patient-003'}), (b:Symptom {id: 'symptom-breathlessness'})
MERGE (charlie)-[:EXPERIENCES {date: '2025-03-02'}]->(b);

// ── 7. Verification queries (comment out when running as an init script) ─
// MATCH (d:Disease)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s:Symptom)
// RETURN d.name AS disease, s.name AS symptom,
//        r.p_given_disease AS pD, r.p_given_no_disease AS pND
// ORDER BY disease, symptom;
//
// MATCH (p:Patient)-[r:EXPERIENCES]->(s:Symptom)
// RETURN p.name AS patient, s.name AS symptom, r.date AS observedOn
// ORDER BY patient, observedOn;
