---
name: bdd-gherkin-validator
description: Guides the TDD/BDD implementation and automated validation of the 54 official Gherkin scenarios of El Chupacabras (HU-001 to HU-015), ensuring exact HTTP status codes, concurrency safety, idempotency, and data invariants.
---

# Skill: BDD / Gherkin & NFR Test Validator

Activate this skill whenever writing or reviewing unit, integration, or BDD acceptance tests in Spring Boot (JUnit 5, MockMvc, Testcontainers, Cucumber) or React (Vitest, React Testing Library).

---

## 1. Official User Story & Gherkin Mapping (54 Scenarios)

Reference files:
- Critical Stories (`HU-001` to `HU-005`, 24 scenarios): [`docs/05-historias-y-criterios/T1-01-historias-criticas-y-gherkin.md`](file:///C:/Users/carlo/Projects/RestaurantTickets/docs/05-historias-y-criterios/T1-01-historias-criticas-y-gherkin.md)
- Complementary Stories (`HU-006` to `HU-015`, 30 scenarios): [`docs/05-historias-y-criterios/T1-02-historias-complementarias-y-gherkin.md`](file:///C:/Users/carlo/Projects/RestaurantTickets/docs/05-historias-y-criterios/T1-02-historias-complementarias-y-gherkin.md)
- Non-Functional Matrix (`RNF-001` to `RNF-018`): [`docs/04-requisitos/T2-01-matriz-requisitos-no-funcionales.md`](file:///C:/Users/carlo/Projects/RestaurantTickets/docs/04-requisitos/T2-01-matriz-requisitos-no-funcionales.md)

| Story ID | Focus Area | Key Scenarios & Edge Cases | Expected HTTP Statuses |
|---|---|---|---|
| **HU-001** | Order Confirmation, Stock & Payment | Cash confirmation, insufficient stock pre-payment, unverified Nequi transfer, concurrent stock conflict, idempotent retry | `201`, `200` (retry), `400`, `403`, `409`, `422` |
| **HU-002** | Daily Operational Inventory | Opening `Jornada` stock, shortage/spoilage adjustment, blocking sales when depleted | `200`, `201`, `403`, `422` |
| **HU-003** | Kitchen Read-Only Queue (`COCINA`) | View paid orders in arrival order, preference visibility, zero financial/address exposure (`RNF-008`), reject state changes by `COCINA` | `200`, `403` |
| **HU-004** | Despacho & Counter/Delivery Handover | Physical receipt from kitchen (consumes stock), adding bottled drinks/packaging, calling by ID + Name, handing to courier | `200`, `403`, `409`, `422` |
| **HU-005** | Cancellations, Incidents & Refunds | Pre-payment withdrawal, pre-prep cancellation with stock release, unauthorized refund rejection, partial refund adjustment | `200`, `403`, `422` |
| **HU-006 - HU-015** | Catalog, Delivery Zones, Daily Close, Audit | Menu & recipes, Los Patios zone fees, failed delivery re-attempts, daily cash/Nequi reconciliation, difference justification | `200`, `201`, `400`, `403`, `422` |

---

## 2. Mandatory Test Assertions by Error & Success Path (Workshop 3)

Every controller/service test suite must verify the exact Workshop 3 error recovery contract ([`docs/02-procesos-as-is-to-be/T3-01-flujo-to-be-tecnico.md`](file:///C:/Users/carlo/Projects/RestaurantTickets/docs/02-procesos-as-is-to-be/T3-01-flujo-to-be-tecnico.md)):

1. **`201 Created` (Happy Path):**
   - Assert atomic persistence of `pedido` (`PAGADO_REGISTRADO`), `pedido_detalle` (commercial snapshot), `pago`, `reserva_inventario` (`ACTIVA`), `historial_estado_pedido`, and `clave_idempotencia`.
2. **`200 OK` (Idempotent Retry - `RNF-010`):**
   - Sending the same `Idempotency-Key` and payload hash returns the previously created `Pedido` with `200 OK` without duplicating `pedido`, `pago`, or `reserva_inventario`.
3. **`400 Bad Request`:**
   - Triggered on missing required fields, zero/negative item quantities, or malformed IDs. Zero database mutations.
4. **`401 Unauthorized` & `403 Forbidden` (`RNF-005`, `RNF-007`):**
   - `401` on expired/missing token (>30 min inactivity).
   - `403` when an unauthorized role attempts an action (e.g., `COCINA` attempting state transition, or `CAJA` attempting to approve a refund/daily close). Assert an audit record is created in `auditoria`.
5. **`409 Conflict` (`RNF-004`):**
   - Triggered when two concurrent requests attempt to reserve the last available quantity of an `insumo`, or when an invalid state transition is requested. Assert transaction rollback and `cantidad_disponible >= 0`.
6. **`422 Unprocessable Entity`:**
   - Triggered on business rule violations: insufficient stock during validation, delivery zone outside Los Patios, incomplete cash amount (`monto_recibido < total`), or Nequi transfer with `transferencia_verificada != true`.

---

## 3. Privacy & Data Minimization Test (`RNF-008`)

When testing endpoints consumed by `COCINA`:
- Explicitly assert via JSONPath (`jsonPath("$.total").doesNotExist()`, `jsonPath("$.pago").doesNotExist()`, `jsonPath("$.direccionTexto").doesNotExist()`) that **zero** financial, payment reference, or delivery address fields are serialized in the response DTO.
