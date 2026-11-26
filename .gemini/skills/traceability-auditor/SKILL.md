---
name: traceability-auditor
description: Audits and certifies the end-to-end bidirectional traceability chain from business problems and rules to requirements (RF/RNF), user stories (HU), Gherkin scenarios, domain aggregates, database tables, and tests per COMP-01-matriz-trazabilidad.md.
---

# Skill: End-to-End Traceability Auditor (`COMP-01`)

Activate this skill when planning a micro-task or verifying its completion against [`docs/04-requisitos/COMP-01-matriz-trazabilidad.md`](file:///C:/Users/carlo/Projects/RestaurantTickets/docs/04-requisitos/COMP-01-matriz-trazabilidad.md).

---

## 1. The Official Traceability Chain

Every line of production and test code in **El Chupacabras** traces back through this chain:

```text
Business Problem (PN-001..PN-010, PA-01..PA-12)
  └──> AS-IS / TO-BE Process (02-procesos-as-is-to-be)
        └──> Business Rule (RN-001..RN-045)
              └──> Functional / Non-Functional Requirement (RF-001..RF-075, RNF-001..RNF-018)
                    └──> User Story (HU-001..HU-015)
                          └──> Gherkin Acceptance Scenario (54 Scenarios)
                                └──> DDD Aggregate / Domain Service (COMP-02)
                                      └──> PostgreSQL Table / Trigger (COMP-04)
                                            └──> Automated Test & Production Code Slice
```

---

## 2. Pre-Implementation & Post-Implementation Audit Checklist

Before marking any micro-task complete, verify and report:

1. **Traceability Identifiers:**
   - Which `HU-xxx`, `RF-xxx`, `RNF-xxx`, and `RN-xxx` are covered by this task?
2. **Test Coverage Proof:**
   - Which Gherkin scenario(s) or unit/integration tests were written **first** (TDD Red-Green-Refactor) to prove this behavior?
   - Were both the happy path and the required failure paths (`400`, `403`, `409`, `422`) tested?
3. **Domain & Data Alignment:**
   - Do the class/method/column names respect the Ubiquitous Language (`COMP-02`) and physical schema (`COMP-04`)?
4. **Proposed Human Commit Message:**
   - Provide the user with a clean, single-line Conventional Commit message referencing the traceability IDs (without any AI attribution or `Co-authored-by` trailers):
     `feat(<context>): <concise summary> [HU-xxx] [RF-xxx] [RN-xxx]`
