# ADR-001: Adopción de Monolito Modular con Arquitectura Hexagonal (Clean Architecture) y DDD

- **Estado:** Aceptado
- **Fecha:** 2026-11-25
- **Requisitos y Reglas Relacionados:** `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-009`, `RNF-014`, `RNF-015`, `COMP-02`

---

## 1. Contexto y Problema

**El Chupacabras** es un restaurante de comida rápida mexicana de autoservicio en Los Patios, Norte de Santander. El sistema **Restaurant Tickets** debe coordinar en tiempo real ocho contextos delimitados (`identidad`, `catalogo`, `inventario`, `pedidos`, `preparacion`, `domicilios`, `incidencias`, `cierre`).

El problema arquitectónico central radica en cumplir simultáneamente con:
1. **Atomicidad transaccional estricta (`RN-016`, `RNF-004`):** Confirmar un pago en Caja exige validar stock, bloquear insumos de la receta, crear el pedido con instantáneas comerciales, registrar el pago y generar la reserva de inventario en una sola operación indivisible con respuesta P95 ≤ 1 s.
2. **Aislamiento y mantenibilidad (`RNF-014`, `RNF-015`):** Un cambio en la política de tarifas de domicilio por zona o en una transición de estados debe quedar encapsulado en máximo 2 módulos de negocio con 0 dependencias circulares.
3. **Continuidad operativa local (`RNF-009`):** El restaurante debe poder seguir vendiendo presencialmente en efectivo aunque se interrumpa el servicio de Internet externo, con consumo de memoria del backend ≤ 512 MB (`RNF-013`).

---

## 2. Alternativas Evaluadas

### Alternativa A: Arquitectura en Capas Tradicional (N-Tier Monolith clásico)
- Agrupar todas las clases en tres paquetes globales (`controllers`, `services`, `repositories`).
- *Desventaja:* Fomenta el acoplamiento transversal ("Big Ball of Mud"), donde servicios de pedidos manipulan directamente tablas de inventario o cierre sin respetar invariantes de dominio, incumpliendo `RNF-014`.

### Alternativa B: Arquitectura de Microservicios Distribuidos
- Desplegar servicios independientes para Pedidos, Inventario, Cocina, Domicilios y Cierre con bases de datos separadas.
- *Desventaja:* Rompe la transaccionalidad ACID local entre `Pedido` y `ReservaInventario`, obligando a implementar Sagas o 2PC con consistencia eventual (riesgo de sobreventa `RNF-004`), multiplica el consumo de RAM por encima del límite de 512 MB (`RNF-013`) y complejiza la operación local en el restaurante (`RNF-009`).

### Alternativa C: Monolito Modular con Arquitectura Hexagonal (Clean Architecture) y DDD *(Seleccionada)*
- Un único artefacto desplegable en Spring Boot 3.4.x dividido por contextos delimitados (`pedidos`, `inventario`, `preparacion`, etc.), donde cada contexto separa `domain`, `application`, `infrastructure` y `web`.

---

## 3. Decisión Tomada

Adoptar un **Monolito Modular con Arquitectura Hexagonal (Ports & Adapters) y Domain-Driven Design (DDD)** sobre Java 17+ y Spring Boot 3.4.5.

1. Cada subdominio identificado en `COMP-02` constituye un paquete raíz independiente bajo `co.elchupacabras.tickets.<contexto>`.
2. Las reglas de negocio (`RN-001` a `RN-045`) y máquinas de estado (`EstadoSolicitud`, `EstadoPedido`) viven en el paquete `domain` sin dependencias de la capa web.
3. La confirmación del pedido y la reserva de insumos se ejecutan dentro de una transacción ACID local (`@Transactional`) gestionada por la capa `application`.

---

## 4. Consecuencias (Trade-offs)

### Positivas (+)
- **Transacciones ACID sin latencia de red:** Cumplimiento garantizado de `RNF-004` (0 reservas dobles y 0 inventarios negativos) y `RNF-002` (P95 ≤ 800 ms) al ejecutar llamadas entre módulos en memoria sobre una misma conexión transaccional PostgreSQL.
- **Alta mantenibilidad y testabilidad (`RNF-014`):** El núcleo de dominio (`domain`) se prueba con tests unitarios puros en milisegundos sin levantar el contexto de Spring.
- **Despliegue simple y resiliente (`RNF-009`, `RNF-013`):** Un solo contenedor Java (`-Xmx512m`) y una instancia PostgreSQL 15+ bastan para operar en red local o VPS.

### Negativas / Compromisos (-)
- **Disciplina de modularidad en el equipo:** Requiere mantener reglas estrictas de visibilidad entre paquetes para evitar que un módulo invoque repositorios JPA internos de otro contexto sin pasar por sus servicios de aplicación o eventos de dominio.
- **Escalado conjunto:** El backend escala como una unidad completa y no por módulo individual (mitigado por el bajo consumo de recursos de cada operación y paginación estricta en `RNF-013`).
