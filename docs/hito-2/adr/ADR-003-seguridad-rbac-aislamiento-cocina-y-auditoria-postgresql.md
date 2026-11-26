# ADR-003: Seguridad RBAC, Minimización de Datos en Cocina (Solo Lectura) y Auditoría Inmutable en PostgreSQL 15+

- **Estado:** Aceptado
- **Fecha:** 2026-11-25
- **Requisitos y Reglas Relacionados:** `RN-019`, `RN-021`, `RN-027`, `RN-038`, `RNF-005`, `RNF-006`, `RNF-007`, `RNF-008`, `RNF-018`

---

## 1. Contexto y Problema

En la operación diaria de **El Chupacabras** participan cinco roles con niveles de responsabilidad muy distintos: `PROPIETARIO`, `CAJA`, `COCINA`, `DESPACHO` y `DOMICILIARIO`.

El diseño de seguridad y persistencia debe resolver cuatro exigencias críticas del dominio:
1. **Aislamiento y Privacidad en Cocina (`RN-019`, `RN-021`, `RN-027`, `RNF-008`):** Cocina opera en una pantalla compartida (KDS) de consulta. Cocina **no puede cambiar estados de pedidos, no puede modificar inventario y tiene prohibido recibir montos de pago, referencias de transferencia Nequi o direcciones de clientes** (0 campos sensibles expuestos en el 100 % de las respuestas JSON del servidor).
2. **Autorización Estricta de Operaciones Financieras (`RN-034`, `RN-038`, `RN-045`, `RNF-005`):** Solo el `PROPIETARIO` puede aprobar devoluciones totales/parciales y cierres de jornada. El 100 % de los intentos no autorizados deben rechazarse con `403 Forbidden` y quedar registrados en auditoría.
3. **Protección de Cuentas y Sesiones (`RNF-006`, `RNF-007`):** Bloqueo temporal de 15 minutos tras 5 intentos fallidos de inicio de sesión y expiración por inactividad a los 30 minutos.
4. **Integridad Relacional y Auditoría (`RNF-018`, `COMP-04`):** El 100 % de las operaciones críticas (creación de pedido, cambio de estado, ajuste de inventario, aprobación de devolución y cierre) deben conservar evidencia inmutable (`datos_anteriores` y `datos_nuevos` en formato `JSONB`) durante al menos 2 años.

---

## 2. Alternativas Evaluadas

### Alternativa A: Filtrado de Datos y Control de Permisos en el Frontend React
- Enviar la entidad `Pedido` completa a todas las vistas de React y ocultar en la interfaz de Cocina los precios, direcciones y botones de transición de estado.
- *Desventaja:* Incumple gravemente `RNF-005` y `RNF-008`. Cualquier inspección de red en el navegador de Cocina expondría montos, referencias Nequi y direcciones de clientes, y permitiría invocar endpoints de cambio de estado mediante herramientas HTTP.

### Alternativa B: Base de Datos NoSQL sin Esquema Físico ni Triggers
- Utilizar una base documental sin tipos enumerados estrictos ni restricciones declarativas.
- *Desventaja:* Pierde la validación transaccional cruzada en el motor de datos (como garantizar que una devolución nunca supere el saldo reembolsable del pedido o que una zona de domicilio pertenezca exclusivamente a Los Patios).

### Alternativa C: Spring Security RBAC + Proyecciones DTO Específicas en Servidor + PostgreSQL 15+ con Triggers PL/pgSQL y Auditoría Event-Driven *(Seleccionada)*
- Aplicar autorización por rol en el backend, construir un DTO de proyección dedicado para Cocina (`ComandaCocinaResponse`), escuchar eventos de dominio con `@EventListener` para escribir en la tabla `auditoria` (`JSONB`), y reforzar invariantes financieras con tipos `ENUM`, `CHECK` constraints y triggers en PostgreSQL 15+.

---

## 3. Decisión Tomada

1. **Control de Acceso Basado en Roles (RBAC) en Backend (`RNF-005`, `RNF-006`, `RNF-007`):**
   - La entidad `Usuario` almacena `hash_contrasena` (BCrypt/Argon2id), `intentos_fallidos` y `bloqueado_hasta` (bloqueo de 15 minutos al alcanzar 5 fallos).
   - Los endpoints validan el rol del actor autenticado. Si `COCINA` intenta invocar `PATCH /api/v1/pedidos/{id}/estado`, o si `CAJA` intenta aprobar una devolución (`POST /api/v1/devoluciones/{id}/aprobar`), el sistema lanza `AccesoDenegadoDominioException`, registra el evento en `auditoria` y devuelve **`403 Forbidden`**.
2. **Proyección de Minimización de Datos para Cocina (`RNF-008`):**
   - Se implementa `ComandaCocinaResponse` en el paquete `preparacion.web.dto`, que expone **exclusivamente**: `codigoPublico`, `modalidad`, `creadoEn` y la lista de ítems (`productoNombre`, `cantidad`, `preferencias`). Ningún campo monetario, de pago o de domicilio existe en este contrato.
3. **Defensa en Profundidad en PostgreSQL 15+ (`COMP-04`) y Auditoría (`RNF-018`):**
   - Se emplea PostgreSQL 15+ con sus 18 tipos `ENUM`, índices parciales (`uq_jornada_abierta_sede`, `uq_pago_principal_pedido`), triggers (`trg_validar_pago_pedido`, `trg_validar_domicilio_pedido`, `trg_validar_saldo_devolucion`) y la tabla append-only `auditoria` alimentada mediante el patrón **Observer (`@EventListener`)**.

---

## 4. Consecuencias (Trade-offs)

### Positivas (+)
- **Cumplimiento del 100 % en privacidad (`RNF-008`) y seguridad (`RNF-005`):** Es imposible que la vista de Cocina filtre datos financieros o de dirección, porque nunca salen del servidor en ese endpoint.
- **Doble barrera de integridad financiera:** Tanto los servicios de dominio en Java como los `CHECK` y triggers de PostgreSQL 15+ impiden pagos en transferencia sin `transferencia_verificada = true` (`RN-008`), zonas fuera de Los Patios (`RN-028`) o devoluciones superiores al pago original (`RN-039`).
- **Trazabilidad forense desacoplada (`RNF-018`):** El uso de `@EventListener` permite registrar auditoría e historial sin ensuciar la lógica principal de los casos de uso.

### Negativas / Compromisos (-)
- **Mantenimiento de DTOs por rol:** Requiere definir DTOs de salida diferenciados para Caja/Despacho frente a Cocina en lugar de reutilizar un único DTO genérico de pedido.
