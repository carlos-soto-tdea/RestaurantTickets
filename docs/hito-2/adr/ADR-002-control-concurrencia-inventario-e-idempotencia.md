# ADR-002: Estrategia de Control de Concurrencia, Reserva Atómica de Inventario e Idempotencia Transaccional

- **Estado:** Aceptado
- **Fecha:** 2026-11-25
- **Requisitos y Reglas Relacionados:** `RN-004`, `RN-015`, `RN-016`, `RN-017`, `RNF-004`, `RNF-010`, `HU-001`, `T3-01`

---

## 1. Contexto y Problema

En hora pico en **El Chupacabras**, dos responsables o terminales de Caja pueden intentar confirmar simultáneamente pedidos pagados que requieren la última porción disponible de un mismo insumo (por ejemplo, los últimos 300 gramos de carne al pastor o la última bebida envasada), o bien la conexión Wi-Fi de la terminal de Caja puede sufrir una micro-caída justo después de enviar `POST /api/v1/pedidos/confirmar`, provocando que el cajero reintente la confirmación.

El sistema debe garantizar dos invariantes críticas:
1. **`RNF-004` (Concurrencia de Inventario):** 0 inventarios negativos y 0 reservas dobles en 1.000 pruebas concurrentes; el primer pedido en confirmar recibe `201 Created` y el segundo recibe `409 Conflict` en ≤ 2 segundos, revirtiendo toda la transacción.
2. **`RNF-010` (Idempotencia en Confirmación):** 0 pedidos o cobros duplicados en 1.000 reintentos con la misma clave `Idempotency-Key`; los reintentos deben devolver `200 OK` con el pedido original.

---

## 2. Alternativas Evaluadas

### Alternativa A: Reserva Temprana al Crear la Solicitud (Pre-pago)
- Bloquear y descontar el inventario desde que se captura la `Solicitud` (`PENDIENTE_PAGO`).
- *Desventaja:* Viola directamente la regla de negocio **`RN-004`** ("Una solicitud cuyo pago no se ha confirmado no reserva inventario"), permitiendo que solicitudes abandonadas por WhatsApp bloqueen el stock real para clientes presenciales.

### Alternativa B: Solo Bloqueo Optimista (`@Version`) sin Bloqueo de Fila ni Tabla de Idempotencia
- Confiar únicamente en `OptimisticLockException` al actualizar `inventario_jornada` y validar duplicados en el frontend React.
- *Desventaja:* Bajo alta contención de pedidos multi-ítem, los fallos optimistas al final de la transacción aumentan los rollbacks tardíos, y si la respuesta HTTP se pierde en red, un reintento con nueva petición crearía un pedido y cobro duplicado.

### Alternativa C: Bloqueo Pesimista Selectivo Ordenado (`SELECT ... FOR UPDATE`) + `@Version` + Tabla Transaccional `clave_idempotencia` *(Seleccionada)*
- Validar sin bloquear durante la fase de `Solicitud` (`RN-004`, `RN-015`), y al confirmar pago (`RN-016`), verificar primero `clave_idempotencia` y luego adquirir bloqueo de fila (`LockModeType.PESSIMISTIC_WRITE`) sobre los insumos requeridos ordenados por `insumo_id ASC` (para prevenir deadlocks), respaldado por restricciones `CHECK (cantidad_disponible >= 0)` en PostgreSQL 15+.

---

## 3. Decisión Tomada

Implementar un mecanismo transaccional de tres capas en `ConfirmadorPedidoService` y PostgreSQL 15+:

1. **Idempotencia Persistente (`clave_idempotencia`):**
   - El endpoint `POST /api/v1/pedidos/confirmar` exige el header `Idempotency-Key`.
   - Antes de procesar el pago o tocar el inventario, se consulta `UNIQUE (usuario_id, operacion, clave)`. Si ya existe con el mismo `hash_solicitud`, el servicio devuelve inmediatamente el resultado almacenado en `respuesta` (`JSONB`) con estado **`200 OK`**.
2. **Bloqueo Pesimista Ordenado + Control Optimista (`@Version`):**
   - Dentro de la transacción `@Transactional`, `InventarioJornadaJpaRepository` ejecuta `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`) ordenando por `insumo_id ASC`.
   - Se verifica nuevamente que `cantidad_disponible >= cantidad_requerida`. Si otro hilo agotó el stock milisegundos antes, se lanza `ConcurrenciaInventarioException`, se hace rollback completo y `@RestControllerAdvice` responde **`409 Conflict`**.
3. **Ciclo de Vida de Reserva (`ACTIVA` -> `CONSUMIDA` | `LIBERADA`):**
   - Al confirmar: `cantidad_disponible -= req`, `cantidad_reservada += req`, y se inserta `reserva_inventario` en estado `ACTIVA`.
   - Al recibir físicamente en Despacho (`RN-016`, `RN-027`): la reserva pasa a `CONSUMIDA` y se registra un `movimiento_inventario` de tipo `CONSUMO`.
   - Al cancelar con autorización antes de preparar (`RN-017`, `RN-034`): la reserva pasa a `LIBERADA` y el stock regresa a `cantidad_disponible`.

---

## 4. Consecuencias (Trade-offs)

### Positivas (+)
- **Cero sobreventa y cero stock negativo:** Imposibilidad matemática y transaccional de vender insumos inexistentes (respaldado además por `CHECK (cantidad_disponible >= 0)` en base de datos).
- **Prevención de interbloqueos (Deadlocks):** Al ordenar siempre los IDs de insumos de menor a mayor antes del `SELECT ... FOR UPDATE`, dos transacciones concurrentes nunca adquieren bloqueos en orden inverso.
- **Seguridad total ante fallos de red (`RNF-010`):** El cajero puede reintentar sin temor a duplicar cobros ni descontar dos veces la receta.

### Negativas / Compromisos (-)
- **Serialización breve sobre insumos compartidos:** Las confirmaciones concurrentes que comparten un mismo ingrediente esperan unos pocos milisegundos la liberación del row-lock (mitigado manteniendo la transacción corta, sin llamadas a servicios externos, completando en < 50 ms).
