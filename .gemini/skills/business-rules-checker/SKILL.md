---
name: business-rules-checker
description: Validates that all code, domain services, APIs, and UI behaviors strictly enforce the 45 business rules of El Chupacabras (RN-001 to RN-045), including strict self-service, manual payment checks, atomic stock reservation, and the order state machine.
---

# Skill: Business Rules Checker (RN-001 to RN-045)

Whenever designing, testing, implementing, or reviewing backend or frontend code, apply this checklist derived from [`docs/03-reglas-de-negocio/01-catalogo-reglas-de-negocio.md`](file:///C:/Users/carlo/Projects/RestaurantTickets/docs/03-reglas-de-negocio/01-catalogo-reglas-de-negocio.md) and [`docs/06-modelo-de-dominio/COMP-02-modelo-de-dominio.md`](file:///C:/Users/carlo/Projects/RestaurantTickets/docs/06-modelo-de-dominio/COMP-02-modelo-de-dominio.md).

---

## 1. Service Model, Channels, Requests & Payments (RN-001 to RN-012)

- [ ] **RN-001 (Self-Service Only):** No table service, no waiters, no table numbers. All operations are counter self-service or delivery.
- [ ] **RN-002 (Allowed Channels):** Only `PRESENCIAL` (in-person at Cashier) and `WHATSAPP` (manually captured by Cashier). Social media is strictly for marketing.
- [ ] **RN-003 (Allowed Payment Methods):** Only `EFECTIVO` (Cash) and `TRANSFERENCIA` (verified in Nequi via Bre-B). Cards, credit, payment gateways, and cash-on-delivery are forbidden (`422 Unprocessable Entity`).
- [ ] **RN-004 (Solicitud vs. Pedido Separation):** A `Solicitud` (`PENDIENTE_PAGO`) is an unpaid quote. It **never** reserves inventory and is not an active order.
- [ ] **RN-005 (Mandatory Order Attributes):** Every confirmed `Pedido` must record: unique `codigo_publico`, timestamp, `canal`, `modalidad`, `nombre_cliente`, product details with commercial snapshots, quantities, customer `preferencias`, `total`, `medio_pago`, and `estado`.
- [ ] **RN-006 (Allowed Modalities):** `CONSUMO_LOCAL`, `PARA_LLEVAR`, `RECOGER`, and `DOMICILIO`.
- [ ] **RN-007 & RN-008 (Payment Confirmation Gate):** An order cannot enter preparation until Cashier confirms payment. For Nequi (`TRANSFERENCIA`), `transferencia_verificada` must be `true` and `referencia_externa` must be non-empty after Cashier manually checks the Nequi app.
- [ ] **RN-009 & RN-010 (Temporary Receipt):** In-person orders require `nombre_cliente` and generate a unique `codigo_publico` for the temporary non-tax receipt.
- [ ] **RN-011 (Calling Order):** Despacho calls in-person orders first by `codigo_publico` and then by `nombre_cliente`.
- [ ] **RN-012 (Preference Integrity):** Customer preferences (e.g., "Sin cebolla", "Salsa aparte") must remain unaltered from `Solicitud` creation to final delivery.

---

## 2. Operational Inventory & Atomic Reservations (RN-013 to RN-019)

- [ ] **RN-013 (Daily Shift Stock):** At the start of each `Jornada`, `PROPIETARIO` (or authorized user) registers initial quantities (`cantidad_inicial`) in `inventario_jornada` for ingredients, bottled drinks (`BEBIDA_ENVASADA`), packaging (`EMPAQUE`), and other supplies.
- [ ] **RN-014 (Product Recipes):** Every sellable product has defined supply requirements (`producto_insumo.cantidad_requerida`).
- [ ] **RN-015 (Pre-Payment Validation):** Before confirming payment (`POST /api/v1/solicitudes/validar`), Cashier validates that `cantidad_disponible` covers all required recipe components, drinks, and packaging. If insufficient, return `422 Unprocessable Entity` listing missing items without reserving anything.
- [ ] **RN-016 (Atomic Reservation & Consumption Lifecycle):**
  - Upon confirming payment (`POST /api/v1/pedidos/confirmar`), lock required `inventario_jornada` rows in a single ACID transaction, verify stock again (return `409 Conflict` if another concurrent order took the last stock), decrement `cantidad_disponible`, increment `cantidad_reservada`, and create `reserva_inventario` with state `ACTIVA`.
  - Reservations remain `ACTIVA` during kitchen preparation and transition to `CONSUMIDA` (with a `CONSUMO` movement in `movimiento_inventario`) when `DESPACHO` physically receives the finished order from `COCINA`.
- [ ] **RN-017 (Reservation Release on Pre-Prep Cancellation):** If a paid order is cancelled with an approved full refund before preparation starts (`PAGADO_REGISTRADO`), release active reservations immediately (`estado = 'LIBERADA'`, return quantity to `cantidad_disponible`, log `LIBERACION` movement).
- [ ] **RN-018 & RN-019 (Kitchen Inventory Isolation):** `COCINA` only sees paid orders with reserved stock. `COCINA` never modifies inventory or orders; physical shortages, spoilage, or waste are reported to Cashier/Propietario to register an incident/adjustment.

---

## 3. Roles, State Machine & Delivery Rules (RN-020 to RN-032)

### Solicitud State Transitions
- `PENDIENTE_PAGO` -> `CANCELADA_SIN_PAGO` (Withdrawn before payment; `RN-033`)
- `PENDIENTE_PAGO` -> `CONVERTIDA` (Converted to `Pedido` upon atomic payment confirmation)

### Pedido State Transitions (`RN-025`, `RN-026`, `RN-027`)

| Source State | Target State | Authorized Role | Trigger / Business Event |
|---|---|---|---|
| *(Confirmation)* | `PAGADO_REGISTRADO` | `CAJA` | Payment confirmed & stock reserved atomically |
| `PAGADO_REGISTRADO` | `EN_PREPARACION` | `CAJA` | Order placed at Kitchen's disposal (`enviar-cocina`) |
| `EN_PREPARACION` | `LISTO_PARA_DESPACHO` | `DESPACHO` | Despacho physically receives finished food from Kitchen (consumes reservations) |
| `LISTO_PARA_DESPACHO` | `LISTO_PARA_RECOGER` | `DESPACHO` | Drinks/packaging added & verified for counter pickup (`CONSUMO_LOCAL`, `PARA_LLEVAR`, `RECOGER`) |
| `LISTO_PARA_DESPACHO` | `EN_RUTA_DOMICILIO` | `DESPACHO` | Handed over to `DOMICILIARIO` (`RN-023`) |
| `LISTO_PARA_RECOGER` | `ENTREGADO` | `DESPACHO` | Handed to customer at counter after calling ID + Name (`RN-011`) |
| `EN_RUTA_DOMICILIO` | `ENTREGADO` | `DOMICILIARIO` | Delivered at customer address in Los Patios (`RN-031`) |
| `EN_RUTA_DOMICILIO` | `NO_ENTREGADO` | `DOMICILIARIO` | Delivery attempt failed with recorded reason (`RN-031`, `RN-032`) |
| `PAGADO_REGISTRADO` | `CANCELADO_REEMBOLSO_PENDIENTE` | `PROPIETARIO` | Approved cancellation before preparation (`RN-034`) |
| `EN_PREPARACION` / `NO_ENTREGADO` | `REEMBOLSO_PENDIENTE` | `PROPIETARIO` | Approved total refund due to business incident (`RN-036`) |
| `CANCELADO_REEMBOLSO_PENDIENTE` / `REEMBOLSO_PENDIENTE` | `REEMBOLSADO` | `PROPIETARIO` / `CAJA` | Full refund executed (`RN-041`) |
| `ENTREGADO` | `ENTREGADO_CON_AJUSTE` | `PROPIETARIO` / `CAJA` | Partial refund executed (`RN-041`) |

- [ ] **RN-021 & RN-027 (Cocina Read-Only):** `COCINA` never executes state transitions (`403 Forbidden`).
- [ ] **RN-028, RN-029, RN-030 (Delivery Constraints):** Deliveries are strictly inside **Los Patios** (`zona_domicilio.municipio = 'Los Patios'`). Customer name, full address, active zone, and delivery fee must be set before confirming payment.

---

## 4. Cancellations, Refunds, Incidents & Daily Close (RN-033 to RN-045)

- [ ] **RN-034 & RN-038 (Propietario Authorization):** Every cancellation of a paid order or total/partial refund requires explicit authorization by `PROPIETARIO` (or designated authority). Unauthorized attempts return `403 Forbidden` and are audited (`RNF-005`).
- [ ] **RN-035 (Verification Before Refund):** Verify order ID, customer name, and payment/contact info before approving a refund.
- [ ] **RN-036 & RN-037 (Incident Attribution):**
  - If attributable to `NEGOCIO` (shortage, prep error, wrong/incomplete item, delivery failure), customer chooses replacement (`REPOSICION`), substitution (`SUSTITUCION`), full refund (`DEVOLUCION_TOTAL`), or partial refund (`DEVOLUCION_PARCIAL`).
  - If failed delivery is attributable to `CLIENTE`, arrange new delivery attempt (with `DOMICILIO_ADICIONAL` fee if applicable) or local pickup (`RECOGIDA_EN_LOCAL`). No refund unless exceptionally authorized by `PROPIETARIO`.
- [ ] **RN-039 & RN-040 (Refund Traceability):** Record order ID, timestamp, reason, authorizer, total/partial amount, original payment method, refund method, and external reference. Cash refunds in cash when possible; transfer refunds via transfer.
- [ ] **RN-042 to RN-045 (Daily Close / Cierre de Jornada):** Reconcile gross sales, executed refunds, net sales (`ventas_brutas - devoluciones_ejecutadas`), expenses (`gasto`), and expected vs. observed totals per payment method (`diferencia_caja`). Any difference or open orders at close require justification and `PROPIETARIO` approval.
