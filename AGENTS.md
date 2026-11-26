# Universal Agent Guidelines: Restaurant Tickets (El Chupacabras)

This repository enforces strict **Agent-Driven Development (ADD)** and **Test-Driven Development (TDD)** conventions. Any AI coding agent operating in this repository (Gemini, Claude, Cursor, Copilot, etc.) must abide by these rules without exception.

---

## 1. Core Domain Rules & Non-Negotiable Scope

1. **Business & Location:**
   - **El Chupacabras**, fast-food Mexican restaurant in **Los Patios, Norte de Santander, Colombia**.
   - **Strict self-service (`RN-001`)**: No waiters, no table service.
   - **Channels (`RN-002`)**: In-person at Cashier (`PRESENCIAL`) and WhatsApp (`WHATSAPP`) manually captured by Cashier.
   - **Payments (`RN-003`, `RN-008`)**: Cash (`EFECTIVO`) and Nequi transfers via Bre-B (`TRANSFERENCIA`) ONLY. Nequi transfers MUST be manually verified by Cashier in the Nequi app (`transferencia_verificada = true` and non-empty `referencia_externa`) before confirming an order.
   - **Strictly Out of Scope**: No DIAN electronic invoicing, no card payment gateways, no cash-on-delivery, no automated WhatsApp bots, no multi-branch operations, no loyalty/points programs.

2. **Dual Lifecycle (`Solicitud` vs. `Pedido`):**
   - `Solicitud` (`PENDIENTE_PAGO` -> `CANCELADA_SIN_PAGO` | `CONVERTIDA`): Pre-validation checks stock and pricing but **NEVER reserves stock** (`RN-004`).
   - `Pedido` (`PAGADO_REGISTRADO`): Created atomically with `Idempotency-Key` upon payment confirmation; locks and reserves required supplies (`reserva_inventario` in state `ACTIVA`) (`RN-016`, `RNF-004`, `RNF-010`).
   - State machine (`RN-026`): `PAGADO_REGISTRADO` -> `EN_PREPARACION` -> `LISTO_PARA_DESPACHO` -> `LISTO_PARA_RECOGER` / `EN_RUTA_DOMICILIO` -> `ENTREGADO` / `NO_ENTREGADO` (plus authorized refund paths: `CANCELADO_REEMBOLSO_PENDIENTE`, `REEMBOLSO_PENDIENTE`, `REEMBOLSADO`, `ENTREGADO_CON_AJUSTE`).
   - **Cocina Isolation (`RN-019`, `RN-021`, `RN-027`, `RNF-008`)**: `COCINA` is strictly read-only. It never changes order state, never alters inventory, and never receives financial totals, payment references, or customer addresses. `DESPACHO` transitions the order to `LISTO_PARA_DESPACHO` upon physical receipt (which marks active inventory reservations as `CONSUMIDA`).

---

## 2. TDD, Atomic Slicing & Git Authorship Policy

1. **Test-Driven Development (Red-Green-Refactor):**
   - Every task starts by writing a failing unit, integration, or Gherkin BDD test (`docs/05-historias-y-criterios/`).
   - Write only the minimal production code required to pass the test, then refactor cleanly.
2. **Atomic Micro-Tasks (No Code Dumps):**
   - Break features into small, verifiable slices (target **≤ 300 lines of code** and **≤ 6 files** per task).
   - Never generate monolithic multi-thousand-line changes or touch unrelated modules in a single turn.
3. **Git Hygiene & Zero AI Attribution:**
   - **NEVER** execute `git commit`, `git commit --amend`, or `git push`.
   - **NEVER** include `Co-authored-by: Gemini` or any AI attribution trailer in proposed commit messages, PRs, or code comments.
   - **NEVER** track `docs/` in Git (it is git-ignored for local AI reference only).
   - Allowed non-destructive Git operations: `git status`, `git diff`, `git log`, `git branch`, `git checkout -b`, `git add`.

---

## 3. Architecture, Folder Structure & Reference Specs

- **Backend (`backend/`):** Spring Boot 3.4.x / Java 17+ DDD Hexagonal Monolith under `co.elchupacabras.tickets.<context>.{domain,application,infrastructure,web}` (`shared`, `identidad`, `catalogo`, `inventario`, `pedidos`, `preparacion`, `domicilios`, `incidencias`, `cierre`).
- **Frontend (`frontend/`):** React 18+ / TypeScript / Vite / Tailwind CSS (`PRE-UIUX-01` tokens) / React Query / Vitest under `frontend/src/{app,shared,features,test}`.
- **Database:** PostgreSQL 15+ (`COMP-04`), Flyway migrations in `backend/src/main/resources/db/migration/`.
- **HTTP Error Contract (`T3-01`):**
  - `200 OK`: Query, state transition, or idempotent retry.
  - `201 Created`: Atomic order confirmation & inventory reservation.
  - `400 Bad Request`: Missing/malformed attributes or quantity ≤ 0.
  - `401 Unauthorized` / `403 Forbidden`: Unauthenticated or unauthorized role (audited in `auditoria`).
  - `409 Conflict`: Concurrent inventory reservation conflict or invalid state transition.
  - `422 Unprocessable Entity`: Business rule failure (insufficient stock pre-payment, address outside Los Patios, incomplete payment, unverified Nequi transfer).
- **Reference Specifications:**
  - Local untracked documentation: [`docs/`](file:///C:/Users/carlo/Projects/RestaurantTickets/docs)
  - Master Gemini instructions: [`GEMINI.md`](file:///C:/Users/carlo/Projects/RestaurantTickets/GEMINI.md)
  - Agent skills: [`.gemini/skills/`](file:///C:/Users/carlo/Projects/RestaurantTickets/.gemini/skills) and [`.agents/skills/`](file:///C:/Users/carlo/Projects/RestaurantTickets/.agents/skills)
