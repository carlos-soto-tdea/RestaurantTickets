---
name: postgres-schema-guard
description: Ensures all JPA/Hibernate entities, SQL repositories, Flyway/Liquibase migrations, triggers, and Spring Boot queries strictly match the PostgreSQL 15+ physical schema (COMP-04) and data dictionary (COMP-05).
---

# Skill: PostgreSQL 15+ Physical Schema Guard (COMP-04 & COMP-05)

Activate this skill whenever creating or modifying JPA entities, database migrations, native queries, or persistence adapters.

---

## 1. Authoritative References

- Physical DDL: [`docs/07-modelo-de-datos/COMP-04-esquema-fisico-postgresql.sql`](file:///C:/Users/carlo/Projects/RestaurantTickets/docs/07-modelo-de-datos/COMP-04-esquema-fisico-postgresql.sql)
- Data Dictionary: [`docs/07-modelo-de-datos/COMP-05-diccionario-de-datos.md`](file:///C:/Users/carlo/Projects/RestaurantTickets/docs/07-modelo-de-datos/COMP-05-diccionario-de-datos.md)
- Conceptual & Logical Model: [`docs/07-modelo-de-datos/COMP-03-modelo-conceptual-y-logico.md`](file:///C:/Users/carlo/Projects/RestaurantTickets/docs/07-modelo-de-datos/COMP-03-modelo-conceptual-y-logico.md)

---

## 2. Exact Native PostgreSQL ENUM Types (17 Types)

Never replace these with arbitrary strings. Preserve exact values:

1. `canal_solicitud`: `PRESENCIAL`, `WHATSAPP`
2. `modalidad_pedido`: `CONSUMO_LOCAL`, `PARA_LLEVAR`, `RECOGER`, `DOMICILIO`
3. `estado_solicitud`: `PENDIENTE_PAGO`, `CANCELADA_SIN_PAGO`, `CONVERTIDA`
4. `estado_pedido`: `PAGADO_REGISTRADO`, `EN_PREPARACION`, `LISTO_PARA_DESPACHO`, `LISTO_PARA_RECOGER`, `EN_RUTA_DOMICILIO`, `ENTREGADO`, `NO_ENTREGADO`, `CANCELADO_REEMBOLSO_PENDIENTE`, `REEMBOLSO_PENDIENTE`, `REEMBOLSADO`, `ENTREGADO_CON_AJUSTE`
5. `medio_pago`: `EFECTIVO`, `TRANSFERENCIA`
6. `tipo_pago`: `PEDIDO`, `DOMICILIO_ADICIONAL`
7. `tipo_producto`: `INDIVIDUAL`, `COMBO`
8. `tipo_insumo`: `INGREDIENTE`, `BEBIDA_ENVASADA`, `EMPAQUE`, `OTRO`
9. `estado_jornada`: `ABIERTA`, `CERRADA`
10. `tipo_movimiento_inventario`: `ENTRADA`, `AJUSTE_POSITIVO`, `AJUSTE_NEGATIVO`, `PERDIDA`, `LIBERACION`, `CONSUMO`
11. `estado_reserva`: `ACTIVA`, `CONSUMIDA`, `LIBERADA`
12. `estado_domicilio`: `PENDIENTE`, `EN_RUTA`, `ENTREGADO`, `NO_ENTREGADO`
13. `resultado_entrega`: `ENTREGADO`, `NO_ENTREGADO`
14. `atribucion_incidencia`: `NEGOCIO`, `CLIENTE`, `EXTERNO`
15. `tipo_incidencia`: `FALTANTE_FISICO`, `DETERIORO`, `DESPERDICIO`, `ERROR_PREPARACION`, `PRODUCTO_INCORRECTO`, `PRODUCTO_INCOMPLETO`, `FALLA_DOMICILIO`, `OTRA`
16. `tipo_resolucion`: `REPOSICION`, `SUSTITUCION`, `DEVOLUCION_TOTAL`, `DEVOLUCION_PARCIAL`, `NUEVO_INTENTO_DOMICILIO`, `RECOGIDA_EN_LOCAL`
17. `tipo_devolucion`: `TOTAL`, `PARCIAL`
18. `estado_devolucion`: `SOLICITADA`, `APROBADA`, `RECHAZADA`, `EJECUTADA`

---

## 3. Exact Table Names & Critical Constraints (25 Tables)

Ensure all table and column names match `COMP-04` verbatim:

| Domain Area | Exact Tables in `COMP-04` | Critical Columns, Triggers & Indexes |
|---|---|---|
| **Identity & Access** | `sede`, `rol`, `usuario`, `usuario_rol` | `rol.codigo IN ('PROPIETARIO', 'CAJA', 'COCINA', 'DESPACHO', 'DOMICILIARIO')`, `intentos_fallidos`, `bloqueado_hasta` (`RNF-006`) |
| **Catalog & Recipes** | `unidad_medida`, `categoria_producto`, `producto`, `insumo`, `producto_insumo` | `factor_base NUMERIC(18,6)`, `precio NUMERIC(14,2)`, `cantidad_requerida NUMERIC(14,3)` |
| **Delivery Zones** | `zona_domicilio` | `ck_zona_municipio CHECK (municipio = 'Los Patios')`, `tarifa NUMERIC(14,2)` |
| **Shift & Inventory** | `jornada`, `inventario_jornada`, `reserva_inventario`, `movimiento_inventario` | Partial unique index `uq_jornada_abierta_sede`, `inventario_jornada.version` (`@Version` optimistic lock), `reserva_inventario.finalizada_en` |
| **Requests & Orders** | `solicitud`, `solicitud_detalle`, `pedido`, `pedido_detalle`, `historial_estado_pedido` | `pedido.codigo_publico UNIQUE`, `pedido.version`, commercial snapshot columns (`producto_codigo`, `producto_nombre`, `precio_unitario`) |
| **Payments** | `pago` | `ck_pago_efectivo`, `ck_pago_vuelto`, partial unique index `uq_pago_principal_pedido`, trigger `trg_validar_pago_pedido` |
| **Deliveries** | `domicilio`, `intento_entrega` | `zona_nombre_snapshot`, `tarifa_aplicada`, `numero_intento`, `costo_adicional`, trigger `trg_validar_domicilio_pedido` |
| **Incidents & Refunds**| `incidencia`, `resolucion_incidencia`, `devolucion` | `producto_sustituto_id`, `medio_pago_original`, `medio_devolucion`, trigger `trg_validar_saldo_devolucion` |
| **Daily Close** | `gasto`, `cierre_jornada`, `diferencia_caja` | `ck_cierre_venta_neta`, `ck_diferencia_calculo CHECK (diferencia = valor_observado - valor_esperado)` |
| **Resilience & Audit** | `clave_idempotencia`, `auditoria` | `UNIQUE (usuario_id, operacion, clave)`, `hash_solicitud`, `respuesta JSONB`, `datos_anteriores JSONB`, `datos_nuevos JSONB`, `direccion_ip INET` |

---

## 4. JPA / Hibernate Mapping Rules

1. **Primary Keys:** `BIGINT GENERATED ALWAYS AS IDENTITY` -> `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
2. **Monetary & Quantity Precision:**
   - Money (`NUMERIC(14,2)`): Always `java.math.BigDecimal` with scale 2.
   - Inventory quantities (`NUMERIC(14,3)`): Always `java.math.BigDecimal` with scale 3.
   - Unit conversion factor (`NUMERIC(18,6)`): Always `java.math.BigDecimal` with scale 6.
   - **Never** use `Double` or `Float` for money or inventory.
3. **Timestamps:** `TIMESTAMPTZ` -> `java.time.OffsetDateTime` or `java.time.Instant`.
4. **Concurrency Control (`RNF-004`):**
   - Map `version BIGINT NOT NULL DEFAULT 0` with `@Version private Long version;` on `inventario_jornada` and `pedido`.
   - Use pessimistic row locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) or `SERIALIZABLE` isolation when reserving `inventario_jornada` rows during order confirmation.
5. **JSONB & INET Columns:**
   - Map `JSONB` columns (`clave_idempotencia.respuesta`, `auditoria.datos_anteriores`, `auditoria.datos_nuevos`) using Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)`.
