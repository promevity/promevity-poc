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

// Alice — 5-Punkte-Timeline für den "Gedächtnis des Graphen"-Aha-Moment
//
//  Datum        Neu               Kumulativ                      Bayes ≈
//  2024-09-10   Fatigue           [Fatigue]                      ~20 % Anemia
//  2024-11-05   Weight Loss       [Fatigue, Weight Loss]         ~28 % Thyroid
//  2025-01-15   Tachykardia       [Fatigue, WL, Tachyk.]         ~45 % Thyroid
//  2025-04-22   Tremor            [Fatigue, WL, Tachyk., Tremor] ~55 % Thyroid
//  2025-08-30   Heat Intolerance  [...alle 5]                    ~65 % Thyroid

MATCH (alice:Patient {id: 'patient-001'}), (f:Symptom {id: 'symptom-fatigue'})
MERGE (alice)-[:EXPERIENCES {date: '2024-09-10'}]->(f);

MATCH (alice:Patient {id: 'patient-001'}), (w:Symptom {id: 'symptom-weight-loss'})
MERGE (alice)-[:EXPERIENCES {date: '2024-11-05'}]->(w);

MATCH (alice:Patient {id: 'patient-001'}), (t:Symptom {id: 'symptom-tachykardia'})
MERGE (alice)-[:EXPERIENCES {date: '2025-01-15'}]->(t);

MATCH (alice:Patient {id: 'patient-001'}), (tr:Symptom {id: 'symptom-tremor'})
MERGE (alice)-[:EXPERIENCES {date: '2025-04-22'}]->(tr);

MATCH (alice:Patient {id: 'patient-001'}), (h:Symptom {id: 'symptom-heat-intolerance'})
MERGE (alice)-[:EXPERIENCES {date: '2025-08-30'}]->(h);

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

// ── 7. Garmin Wearable — VitalMeasurement nodes + HAS_MEASUREMENT edges ──
//    (:Patient)-[:HAS_MEASUREMENT]->(:VitalMeasurement {
//        id, type, value, unit, recordedAt, source
//    })
//    Timestamps: 2026-02-22 (letzte 365 Tage ab Demo-Datum 2026-02-23)
//    Kalibriert für die Schwellenwerte des VitalSignInterpreter:
//      HR > 100 bpm → Tachykardia | HRV < 30 ms → Tachykardia
//      SpO₂ < 94 % → Breathlessness | Stress > 70 → Anxiety | Schlaf < 6 h → Fatigue

// — Alice — Hyperthyreose-Muster: hohe HR + niedriger HRV + kurzer Schlaf
MERGE (v1:VitalMeasurement {id: 'vital-alice-hr-01'})
ON CREATE SET v1.type = 'HEART_RATE', v1.value = 108.0, v1.unit = 'bpm',
              v1.recordedAt = '2026-02-22T07:30:00', v1.source = 'Garmin';
MATCH (p:Patient {id: 'patient-001'}), (v:VitalMeasurement {id: 'vital-alice-hr-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

MERGE (v2:VitalMeasurement {id: 'vital-alice-hrv-01'})
ON CREATE SET v2.type = 'HRV', v2.value = 24.0, v2.unit = 'ms',
              v2.recordedAt = '2026-02-22T07:30:00', v2.source = 'Garmin';
MATCH (p:Patient {id: 'patient-001'}), (v:VitalMeasurement {id: 'vital-alice-hrv-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

MERGE (v3:VitalMeasurement {id: 'vital-alice-sleep-01'})
ON CREATE SET v3.type = 'SLEEP_DURATION_HOURS', v3.value = 5.2, v3.unit = 'h',
              v3.recordedAt = '2026-02-22T06:00:00', v3.source = 'Garmin';
MATCH (p:Patient {id: 'patient-001'}), (v:VitalMeasurement {id: 'vital-alice-sleep-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

MERGE (v4:VitalMeasurement {id: 'vital-alice-spo2-01'})
ON CREATE SET v4.type = 'SPO2', v4.value = 98.1, v4.unit = '%',
              v4.recordedAt = '2026-02-22T07:31:00', v4.source = 'Garmin';
MATCH (p:Patient {id: 'patient-001'}), (v:VitalMeasurement {id: 'vital-alice-spo2-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

MERGE (v5:VitalMeasurement {id: 'vital-alice-stress-01'})
ON CREATE SET v5.type = 'STRESS_LEVEL', v5.value = 65.0, v5.unit = 'score',
              v5.recordedAt = '2026-02-22T12:00:00', v5.source = 'Garmin';
MATCH (p:Patient {id: 'patient-001'}), (v:VitalMeasurement {id: 'vital-alice-stress-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

// — Bob — Anämie-Muster: niedrige SpO₂, normale HR, wenig Schritte
MERGE (v6:VitalMeasurement {id: 'vital-bob-spo2-01'})
ON CREATE SET v6.type = 'SPO2', v6.value = 91.5, v6.unit = '%',
              v6.recordedAt = '2026-02-22T08:00:00', v6.source = 'Garmin';
MATCH (p:Patient {id: 'patient-002'}), (v:VitalMeasurement {id: 'vital-bob-spo2-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

MERGE (v7:VitalMeasurement {id: 'vital-bob-hr-01'})
ON CREATE SET v7.type = 'HEART_RATE', v7.value = 88.0, v7.unit = 'bpm',
              v7.recordedAt = '2026-02-22T08:00:00', v7.source = 'Garmin';
MATCH (p:Patient {id: 'patient-002'}), (v:VitalMeasurement {id: 'vital-bob-hr-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

MERGE (v8:VitalMeasurement {id: 'vital-bob-steps-01'})
ON CREATE SET v8.type = 'STEPS_PER_DAY', v8.value = 2800.0, v8.unit = 'steps',
              v8.recordedAt = '2026-02-22T23:59:00', v8.source = 'Garmin';
MATCH (p:Patient {id: 'patient-002'}), (v:VitalMeasurement {id: 'vital-bob-steps-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

MERGE (v9:VitalMeasurement {id: 'vital-bob-sleep-01'})
ON CREATE SET v9.type = 'SLEEP_DURATION_HOURS', v9.value = 8.3, v9.unit = 'h',
              v9.recordedAt = '2026-02-22T06:30:00', v9.source = 'Garmin';
MATCH (p:Patient {id: 'patient-002'}), (v:VitalMeasurement {id: 'vital-bob-sleep-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

// — Charlie — Herzrhythmusstörungs-Muster: sehr hohe HR + sehr niedriger HRV + hoher Stress
MERGE (v10:VitalMeasurement {id: 'vital-charlie-hr-01'})
ON CREATE SET v10.type = 'HEART_RATE', v10.value = 121.0, v10.unit = 'bpm',
              v10.recordedAt = '2026-02-22T09:15:00', v10.source = 'Garmin';
MATCH (p:Patient {id: 'patient-003'}), (v:VitalMeasurement {id: 'vital-charlie-hr-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

MERGE (v11:VitalMeasurement {id: 'vital-charlie-hrv-01'})
ON CREATE SET v11.type = 'HRV', v11.value = 16.0, v11.unit = 'ms',
              v11.recordedAt = '2026-02-22T09:15:00', v11.source = 'Garmin';
MATCH (p:Patient {id: 'patient-003'}), (v:VitalMeasurement {id: 'vital-charlie-hrv-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

MERGE (v12:VitalMeasurement {id: 'vital-charlie-stress-01'})
ON CREATE SET v12.type = 'STRESS_LEVEL', v12.value = 78.0, v12.unit = 'score',
              v12.recordedAt = '2026-02-22T12:00:00', v12.source = 'Garmin';
MATCH (p:Patient {id: 'patient-003'}), (v:VitalMeasurement {id: 'vital-charlie-stress-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

MERGE (v13:VitalMeasurement {id: 'vital-charlie-spo2-01'})
ON CREATE SET v13.type = 'SPO2', v13.value = 95.8, v13.unit = '%',
              v13.recordedAt = '2026-02-22T09:16:00', v13.source = 'Garmin';
MATCH (p:Patient {id: 'patient-003'}), (v:VitalMeasurement {id: 'vital-charlie-spo2-01'})
MERGE (p)-[:HAS_MEASUREMENT]->(v);

// ── 8. Verification queries (comment out when running as an init script) ─
// MATCH (d:Disease)-[r:HAS_SYMPTOM_LIKELIHOOD]->(s:Symptom)
// RETURN d.name AS disease, s.name AS symptom,
//        r.p_given_disease AS pD, r.p_given_no_disease AS pND
// ORDER BY disease, symptom;
//
// MATCH (p:Patient)-[r:EXPERIENCES]->(s:Symptom)
// RETURN p.name AS patient, s.name AS symptom, r.date AS observedOn
// ORDER BY patient, observedOn;
