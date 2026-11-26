# 1. Vista Arquitectónica y Modelo C4 (Diagrams as Code)

## 1.1 Justificación del Estilo Arquitectónico

Para el sistema **Restaurant Tickets ("El Chupacabras")** se adoptó una arquitectura de **Monolito Modular con Arquitectura Hexagonal (Ports & Adapters / Clean Architecture) y Domain-Driven Design (DDD)** sobre un único artefacto desplegable en **Spring Boot 3.4.5 / Java 17+** y **PostgreSQL 15+**, organizado internamente en **8 Contextos Delimitados (Bounded Contexts)** (`identidad`, `catalogo`, `inventario`, `pedidos`, `preparacion`, `domicilios`, `incidencias`, `cierre`) y un módulo transversal (`shared`).

Cada contexto delimitado separa estrictamente sus responsabilidades en cuatro capas:
1. **`domain` (Núcleo de Dominio):** Entidades de negocio puras, Agregados (`Solicitud`, `Pedido`, `Jornada`, `InventarioJornada`, `Devolucion`), Objetos de Valor, las 18 Enumeraciones del negocio (`EstadoPedido`, `EstadoSolicitud`, `MedioPago`, `TipoInsumo`, etc.) y Estrategias de dominio sin acoplamiento a frameworks web o detalles de infraestructura.
2. **`application` (Casos de Uso y Orquestación Transaccional):** Servicios de aplicación (`@Service`, `@Transactional`), coordinación entre módulos y publicación/escucha de eventos internos de dominio (`ApplicationEventPublisher` / `@EventListener`).
3. **`infrastructure` (Adaptadores Secundarios / Persistencia):** Entidades ORM (`@Entity`), repositorios Spring Data JPA (`JpaRepository`) con bloqueo pesimista (`LockModeType.PESSIMISTIC_WRITE`) y optimista (`@Version`), configuración de seguridad RBAC y migraciones versionadas con Flyway hacia PostgreSQL 15+.
4. **`web` (Adaptadores Primarios / API REST):** Controladores REST (`@RestController`), DTOs de entrada/salida con validaciones Jakarta Bean Validation (`@Valid`) y manejo centralizado de errores (`@RestControllerAdvice`).

| Atributo de Calidad (RNF) | Escenario y Métrica Cuantificable Exigida | Respuesta Arquitectónica del Monolito Modular Hexagonal |
|---|---|---|
| **Integridad y Concurrencia (`RNF-004`, `RNF-010`)** | 0 inventarios negativos y 0 reservas dobles en 1.000 confirmaciones concurrentes (`409 Conflict` <= 2 s); 0 pedidos duplicados en 1.000 reintentos con `Idempotency-Key` (`200 OK` <= 2 s). | Al residir `pedidos` e `inventario` en el mismo proceso y base de datos PostgreSQL 15+, la confirmación de pago (`POST /api/v1/pedidos/confirmar`) se ejecuta en una **única transacción ACID local (`@Transactional`)** combinando bloqueo selectivo de filas (`SELECT ... FOR UPDATE` / `LockModeType.PESSIMISTIC_WRITE`), control optimista (`@Version`) y verificación en la tabla `clave_idempotencia`, evitando la complejidad y latencia de transacciones distribuidas (2PC/Sagas). |
| **Rendimiento (`RNF-001`, `RNF-002`, `RNF-003`)** | Consulta de catálogo/disponibilidad P95 <= 1 s; validación de receta (hasta 30 ítems y 200 insumos) P95 <= 800 ms; transición de estado en despacho P95 <= 1 s. | Comunicación en memoria (in-process) entre módulos del monolito sin saltos de red RPC entre microservicios; carga perezosa (`FetchType.LAZY`) en todas las relaciones JPA; consultas proyectadas con `JOIN FETCH` e índices compuestos en PostgreSQL 15+ y caché de corta duración en React Query. |
| **Seguridad y Privacidad (`RNF-005`, `RNF-006`, `RNF-007`, `RNF-008`)** | 100 % de intentos no autorizados rechazados con `403 Forbidden` y auditados; bloqueo de cuenta 15 min tras 5 fallos (`RNF-006`); expiración de sesión a los 30 min (`RNF-007`); **0 campos financieros o de dirección expuestos a Cocina (`RNF-008`)**. | Filtro de seguridad centralizado con RBAC por rol (`PROPIETARIO`, `CAJA`, `COCINA`, `DESPACHO`, `DOMICILIARIO`). El módulo `preparacion` expone un **DTO de proyección exclusivo para Cocina (`ComandaCocinaResponse`)** construido en el servidor que omite precios, pagos, referencias Nequi y direcciones. |
| **Mantenibilidad y Evolución (`RNF-014`, `RNF-015`)** | Cambios en tarifas de zona o reglas de transición aislados en máximo 2 módulos, 0 dependencias circulares y 100 % de compatibilidad con pedidos históricos. | La Arquitectura Hexagonal aísla las reglas (`RN-001` a `RN-045`) en el paquete `domain` y aplica los patrones **Strategy** (verificación de pagos Efectivo vs. Nequi) y **Observer** (auditoría e historial de estados). Las instantáneas comerciales (`producto_nombre`, `precio_unitario`, `zona_nombre_snapshot`) garantizan inmutabilidad histórica. |
| **Disponibilidad Local y Recuperación (`RNF-009`, `RNF-011`, `RNF-013`)** | 100 % de continuidad en ventas presenciales en efectivo ante caída de Internet externo; `RPO = 0` y `RTO <= 10 min`; consumo RAM backend <= 512 MB. | El despliegue de un monolito autocontenido (`-Xmx512m`) junto a PostgreSQL 15+ en red local o VPS único permite operar sin depender de pasarelas de pago externas en la nube. |

---

## 1.2 Nivel 1 — Diagrama de Contexto del Sistema (C4 Level 1)

### 1.2.1 Vista de Interacción con Clientes, Caja y Sistemas Externos

```mermaid
flowchart TB
    CLIENTE["Cliente<br/>Presencial o WhatsApp<br/>Autoservicio RN-001"]

    subgraph Externos["Canales y Medios Externos"]
        WHATSAPP["WhatsApp<br/>Captura manual RN-002"]
        NEQUI["Nequi / Bre-B<br/>Verificacion manual RN-008"]
    end

    CAJA["Responsable de Caja<br/>Rol: CAJA"]
    SISTEMA["Sistema Restaurant Tickets<br/>El Chupacabras - Los Patios"]

    CLIENTE -->|"1. Envia solicitud"| WHATSAPP
    CLIENTE -->|"2. Transfiere pago"| NEQUI
    CLIENTE -->|"3. Paga efectivo / Reclama"| CAJA
    WHATSAPP -->|"Lee solicitud"| CAJA
    NEQUI -->|"Verifica ingreso y ref"| CAJA
    CAJA -->|"HTTPS JSON: Valida y confirma"| SISTEMA
```

### 1.2.2 Vista de Roles Operativos Internos y el Sistema Central

```mermaid
flowchart TB
    PROPIETARIO["Propietario<br/>Rol: PROPIETARIO<br/>Jornada, stock, devoluciones y cierre"]
    CAJA["Responsable de Caja<br/>Rol: CAJA<br/>Solicitudes, pagos y envio a cocina"]
    SISTEMA["Sistema Restaurant Tickets<br/>El Chupacabras - Los Patios"]
    COCINA["Personal de Cocina<br/>Rol: COCINA - Solo lectura<br/>Sin precios ni direcciones RNF-008"]
    DESPACHO["Responsable de Despacho<br/>Rol: DESPACHO<br/>Recibe orden, consume reserva y entrega"]
    DOMICILIARIO["Domiciliario<br/>Rol: DOMICILIARIO<br/>Entregas en Los Patios RN-028"]

    PROPIETARIO -->|"HTTPS JSON: Administracion"| SISTEMA
    CAJA -->|"HTTPS JSON: Ventas y cobros"| SISTEMA
    SISTEMA -->|"HTTPS JSON: Comandas pagadas"| COCINA
    DESPACHO -->|"HTTPS JSON: Recepcion y llamado"| SISTEMA
    DOMICILIARIO -->|"HTTPS JSON: Reporte entrega"| SISTEMA
```

---

## 1.3 Nivel 2 — Diagrama de Contenedores (C4 Level 2)

```mermaid
flowchart TB
    ACTORES["Actores Operativos<br/>Caja | Cocina | Despacho | Domiciliario | Propietario"]

    subgraph Sistema["Sistema Restaurant Tickets - El Chupacabras"]
        PROXY["Nginx 1.27 Reverse Proxy<br/>Terminacion TLS/SSL HTTPS 443"]
        SPA["SPA Frontend: React 18.3 + TypeScript<br/>Tailwind CSS + React Query + Idempotency-Key"]
        API["API REST Backend: Spring Boot 3.4.5<br/>Java 17 + Spring Security + JPA + Flyway"]
        DB[("Base de Datos: PostgreSQL 15+<br/>18 ENUMs, 30 Tablas, Triggers y Auditoria")]
    end

    ACTORES -->|"1. HTTPS 443"| PROXY
    PROXY -->|"2. Sirve SPA estatica"| SPA
    SPA -->|"3. REST JSON + JWT + Idempotency-Key"| PROXY
    PROXY -->|"4. HTTP 8080 /api/v1"| API
    API -->|"5. JDBC TCP 5432 ACID"| DB
```

---

## 1.4 Nivel 3 — Diagrama de Componentes del Backend (C4 Level 3)

### 1.4.1 Vista de Seguridad, Manejo de Errores y Controladores Web

```mermaid
flowchart TB
    SPA["SPA Frontend React 18.3"]

    subgraph Capa_Seguridad["1. Seguridad y Errores - shared"]
        SEC["SecurityConfig y RBAC Filter<br/>Sesion 30m RNF-007 y Bloqueo RNF-006"]
        ERR["GlobalExceptionHandler<br/>RestControllerAdvice: 400, 403, 409, 422"]
    end

    subgraph Capa_Web["2. Controladores REST - web"]
        SOL_C["SolicitudController<br/>POST /api/v1/solicitudes/validar"]
        PED_C["PedidoController<br/>POST /api/v1/pedidos/confirmar<br/>PATCH /api/v1/pedidos/id/estado"]
        COC_C["CocinaQueryController<br/>GET /api/v1/cocina/comandas"]
    end

    subgraph Capa_App["3. Servicios de Aplicacion - application"]
        VAL_S["ValidadorDisponibilidadService"]
        CONF_S["ConfirmadorPedidoService"]
        COC_S["ConsultaComandasCocinaService"]
    end

    SPA -->|"HTTPS JSON"| SEC
    SEC --> SOL_C
    SEC --> PED_C
    SEC --> COC_C
    SOL_C --> VAL_S
    PED_C --> CONF_S
    COC_C --> COC_S
    SOL_C -.-> ERR
    PED_C -.-> ERR
```

### 1.4.2 Vista de Servicios de Aplicación, Patrones de Dominio y Persistencia JPA

```mermaid
flowchart TB
    subgraph Capa_App["3. Servicios y Patrones - application y domain"]
        VAL_S["ValidadorDisponibilidadService<br/>Recetas RN-014 y Zonas Los Patios RN-028"]
        CONF_S["ConfirmadorPedidoService - Transactional<br/>Idempotencia RNF-010 y Reserva RN-016"]
        COC_S["ConsultaComandasCocinaService<br/>DTO sin precios RNF-008"]
        STRAT["Strategy: ValidadorPagoStrategy<br/>EfectivoPago y TransferenciaNequiPago"]
        OBS["Observer: AuditoriaHistorialEventListener<br/>EventListener CambioEstadoPedidoEvent"]
    end

    subgraph Capa_Infra["4. Repositorios JPA - infrastructure"]
        CAT_R["ProductoInsumoJpaRepository<br/>JOIN FETCH FetchType.LAZY"]
        INV_R["InventarioJornadaJpaRepository<br/>Lock PESSIMISTIC_WRITE y Version"]
        PED_R["PedidoJpaRepository y PagoJpaRepository"]
        IDEM_R["ClaveIdempotencia y AuditoriaRepository"]
    end

    PG[("PostgreSQL 15+")]

    CONF_S --> VAL_S
    CONF_S --> STRAT
    CONF_S -.->|"Evento Dominio"| OBS
    VAL_S --> CAT_R
    VAL_S --> INV_R
    CONF_S --> INV_R
    CONF_S --> PED_R
    CONF_S --> IDEM_R
    COC_S --> PED_R
    OBS --> IDEM_R
    CAT_R --> PG
    INV_R --> PG
    PED_R --> PG
    IDEM_R --> PG
```
