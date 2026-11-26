# 2. Modelado UML Esencial para Producción

## 2.1 Diagrama de Clases de Dominio (UML Class Diagram)

Para garantizar total legibilidad, el modelo de clases de dominio se organiza en cuatro vistas modulares cohesivas que cubren la totalidad de entidades, atributos tipados, métodos, enumeraciones y cardinalidades (`1:1`, `1:N`, `N:M`).

### 2.1.1 Subsistema de Identidad, Sede, Jornada y Enumeraciones Principales

```mermaid
classDiagram
    direction TB

    class EstadoJornada {
        <<enumeration>>
        ABIERTA
        CERRADA
    }

    class CanalSolicitud {
        <<enumeration>>
        PRESENCIAL
        WHATSAPP
    }

    class ModalidadPedido {
        <<enumeration>>
        CONSUMO_LOCAL
        PARA_LLEVAR
        RECOGER
        DOMICILIO
    }

    class MedioPago {
        <<enumeration>>
        EFECTIVO
        TRANSFERENCIA
    }

    class Sede {
        +Long id
        +String nombre
        +String municipio
        +String departamento
        +boolean activa
    }

    class Usuario {
        +Long id
        +String nombreUsuario
        +String nombreCompleto
        +String hashContrasena
        +boolean activo
        +short intentosFallidos
        +OffsetDateTime bloqueadoHasta
        +registrarIntentoFallido() void
        +estaBloqueado() boolean
        +tieneRol(String codigo) boolean
    }

    class Rol {
        +Long id
        +String codigo
        +String nombre
    }

    class Jornada {
        +Long id
        +LocalDate fechaOperacion
        +EstadoJornada estado
        +OffsetDateTime abiertaEn
        +OffsetDateTime cerradaEn
        +cerrarJornada() void
    }

    Usuario "*" -- "*" Rol : usuario_rol N..M
    Sede "1" --> "0..*" Jornada : opera 1..N
    Usuario "1" --> "0..*" Jornada : abre 1..N
    Jornada ..> EstadoJornada
```

### 2.1.2 Subsistema de Catálogo, Recetas e Inventario Operativo por Jornada

```mermaid
classDiagram
    direction TB

    class TipoInsumo {
        <<enumeration>>
        INGREDIENTE
        BEBIDA_ENVASADA
        EMPAQUE
        OTRO
    }

    class EstadoReserva {
        <<enumeration>>
        ACTIVA
        CONSUMIDA
        LIBERADA
    }

    class CategoriaProducto {
        +Long id
        +String nombre
        +boolean activa
    }

    class Producto {
        +Long id
        +String codigo
        +String nombre
        +String tipo
        +BigDecimal precio
        +boolean activo
    }

    class UnidadMedida {
        +Long id
        +String codigo
        +BigDecimal factorBase
    }

    class Insumo {
        +Long id
        +String codigo
        +String nombre
        +TipoInsumo tipo
        +BigDecimal umbralBajo
    }

    class ProductoInsumo {
        +Long productoId
        +Long insumoId
        +BigDecimal cantidadRequerida
    }

    class InventarioJornada {
        +Long id
        +BigDecimal cantidadInicial
        +BigDecimal cantidadDisponible
        +BigDecimal cantidadReservada
        +Long version
        +reservar(BigDecimal cant) void
        +consumirReserva(BigDecimal cant) void
        +liberarReserva(BigDecimal cant) void
    }

    class ReservaInventario {
        +Long id
        +BigDecimal cantidad
        +EstadoReserva estado
        +marcarConsumida() void
        +marcarLiberada() void
    }

    class MovimientoInventario {
        +Long id
        +String tipo
        +BigDecimal cantidad
        +String motivo
    }

    CategoriaProducto "1" --> "0..*" Producto : clasifica 1..N
    UnidadMedida "1" --> "0..*" Insumo : mide 1..N
    Producto "1" *-- "1..*" ProductoInsumo : receta 1..N
    Insumo "1" --> "0..*" ProductoInsumo : compone 1..N
    Insumo "1" --> "0..*" InventarioJornada : stock diario 1..N
    InventarioJornada "1" --> "0..*" ReservaInventario : compromete 1..N
    InventarioJornada "1" --> "0..*" MovimientoInventario : kardex 1..N
```

### 2.1.3 Subsistema Nuclear de Solicitudes, Pedidos, Pagos e Idempotencia

```mermaid
classDiagram
    direction TB

    class EstadoSolicitud {
        <<enumeration>>
        PENDIENTE_PAGO
        CANCELADA_SIN_PAGO
        CONVERTIDA
    }

    class EstadoPedido {
        <<enumeration>>
        PAGADO_REGISTRADO
        EN_PREPARACION
        LISTO_PARA_DESPACHO
        LISTO_PARA_RECOGER
        EN_RUTA_DOMICILIO
        ENTREGADO
        NO_ENTREGADO
        CANCELADO_REEMBOLSO_PENDIENTE
        REEMBOLSO_PENDIENTE
        REEMBOLSADO
        ENTREGADO_CON_AJUSTE
    }

    class Solicitud {
        +Long id
        +CanalSolicitud canal
        +ModalidadPedido modalidad
        +String nombreCliente
        +BigDecimal subtotal
        +BigDecimal costoDomicilio
        +BigDecimal total
        +EstadoSolicitud estado
        +marcarConvertida() void
    }

    class SolicitudDetalle {
        +Long id
        +int cantidad
        +BigDecimal precioUnitario
        +String preferencias
        +BigDecimal subtotal
    }

    class Pedido {
        +Long id
        +String codigoPublico
        +CanalSolicitud canal
        +ModalidadPedido modalidad
        +String nombreCliente
        +BigDecimal subtotal
        +BigDecimal costoDomicilio
        +BigDecimal total
        +EstadoPedido estado
        +Long version
        +enviarACocina(String rol) void
        +recibirEnDespacho(String rol) void
        +cambiarEstado(EstadoPedido nuevo, String rol) void
    }

    class PedidoDetalle {
        +Long id
        +String productoCodigo
        +String productoNombre
        +int cantidad
        +BigDecimal precioUnitario
        +String preferencias
        +BigDecimal subtotal
    }

    class Pago {
        +Long id
        +String tipo
        +MedioPago medio
        +BigDecimal montoPagado
        +BigDecimal montoRecibido
        +BigDecimal vuelto
        +Boolean transferenciaVerificada
        +String referenciaExterna
    }

    class HistorialEstadoPedido {
        +Long id
        +EstadoPedido estadoAnterior
        +EstadoPedido estadoNuevo
        +String origen
    }

    class ClaveIdempotencia {
        +Long id
        +String operacion
        +String clave
        +Short codigoRespuesta
        +String respuestaJson
    }

    Solicitud "1" *-- "1..*" SolicitudDetalle : items 1..N
    Solicitud "1" --> "0..1" Pedido : convierte en 1..1
    Pedido "1" *-- "1..*" PedidoDetalle : snapshot 1..N
    Pedido "1" *-- "1..*" Pago : pagos 1..N
    Pedido "1" *-- "1..*" HistorialEstadoPedido : historial 1..N
    Pedido "0..1" <-- "0..*" ClaveIdempotencia : idempotencia 1..N
```

### 2.1.4 Subsistema de Domicilios en Los Patios, Incidencias, Devoluciones y Cierre

```mermaid
classDiagram
    direction TB

    class EstadoDomicilio {
        <<enumeration>>
        PENDIENTE
        EN_RUTA
        ENTREGADO
        NO_ENTREGADO
    }

    class EstadoDevolucion {
        <<enumeration>>
        SOLICITADA
        APROBADA
        RECHAZADA
        EJECUTADA
    }

    class ZonaDomicilio {
        +Long id
        +String nombre
        +String municipio
        +BigDecimal tarifa
        +boolean activa
        +esCoberturaValidaLosPatios() boolean
    }

    class Domicilio {
        +Long id
        +String direccionTexto
        +String zonaNombreSnapshot
        +BigDecimal tarifaAplicada
        +EstadoDomicilio estado
    }

    class IntentoEntrega {
        +Long id
        +short numeroIntento
        +String resultado
        +String motivoNoEntrega
        +BigDecimal costoAdicional
    }

    class Incidencia {
        +Long id
        +String tipo
        +String atribucion
        +String descripcion
    }

    class Devolucion {
        +Long id
        +String tipo
        +EstadoDevolucion estado
        +String motivo
        +BigDecimal monto
        +MedioPago medioDevolucion
        +aprobar(String rolActor) void
        +ejecutar(String referencia) void
    }

    class CierreJornada {
        +Long id
        +BigDecimal ventasBrutas
        +BigDecimal devolucionesEjecutadas
        +BigDecimal ventasNetas
        +BigDecimal gastosTotales
    }

    class DiferenciaCaja {
        +Long id
        +MedioPago medio
        +BigDecimal valorEsperado
        +BigDecimal valorObservado
        +BigDecimal diferencia
    }

    ZonaDomicilio "1" --> "0..*" Domicilio : aplica tarifa 1..N
    Domicilio "1" *-- "1..*" IntentoEntrega : intentos 1..N
    Pedido "1" --> "0..1" Domicilio : despacho 1..1
    Pedido "1" --> "0..*" Incidencia : reporta 1..N
    Pedido "1" --> "0..*" Devolucion : reembolsa 1..N
    CierreJornada "1" *-- "1..2" DiferenciaCaja : arqueo 1..N
```

---

## 2.2 Diagrama de Secuencia Full Stack (Flujo Transaccional Crítico)

### 2.2.1 Fase 1: Captura y Validación Previa de Solicitud (Sin Reserva de Stock — `RN-004`, `RN-015`, `RN-028`)

```mermaid
sequenceDiagram
    autonumber
    actor Caja as Caja
    participant React as React SPA
    participant Ctrl as SolicitudController
    participant Advice as RestControllerAdvice
    participant Svc as ValidadorService
    participant Repo as JPA Repositories
    participant DB as PostgreSQL 15+

    Caja->>React: Captura items, preferencias y zona
    React->>Ctrl: POST /api/v1/solicitudes/validar

    alt 400 Bad Request - Campos ausentes o cantidad <= 0
        Ctrl-->>Advice: MethodArgumentNotValidException
        Advice-->>React: HTTP 400 DATOS_INVALIDOS
        React-->>Caja: Marca error y conserva formulario
    else Datos sintacticamente validos
        Ctrl->>Svc: validarYCrearSolicitud(request)
        Svc->>Repo: findRecetasConInsumos + findInventarioJornada
        Repo->>DB: SELECT stock disponible y zona Los Patios
        DB-->>Repo: Existencias y tarifa

        alt 422 Unprocessable Entity - Stock insuficiente o zona fuera de Los Patios
            Svc-->>Advice: ReglaNegocioException
            Advice-->>React: HTTP 422 STOCK_INSUFICIENTE o ZONA_FUERA_COBERTURA
            React-->>Caja: Informa faltante sin reservar stock RN-004
        else 200 OK - Solicitud viable
            Svc->>Repo: save(Solicitud PENDIENTE_PAGO)
            Repo->>DB: INSERT INTO solicitud y solicitud_detalle
            Svc-->>Ctrl: SolicitudValidadaResponse
            Ctrl-->>React: HTTP 200 OK (subtotal, costoDomicilio, total)
            React-->>Caja: Muestra total y habilita cobro
        end
    end
```

### 2.2.2 Fase 2: Verificación de Pago, Idempotencia y Reserva Atómica de Inventario (`RN-008`, `RN-016`, `RNF-004`, `RNF-010`)

```mermaid
sequenceDiagram
    autonumber
    actor Caja as Caja
    participant React as React SPA
    participant Ctrl as PedidoController
    participant Advice as RestControllerAdvice
    participant Svc as ConfirmadorService
    participant Strat as PagoStrategy
    participant Repo as JPA Repositories
    participant DB as PostgreSQL 15+

    Note over Caja,React: En Nequi Bre-B, Caja verifica app Nequi primero RN-008
    Caja->>React: Registra pago y pulsa Confirmar
    React->>Ctrl: POST /api/v1/pedidos/confirmar (Idempotency-Key)

    alt 403 Forbidden - Rol no autorizado RNF-005
        Ctrl-->>Advice: AccesoDenegadoDominioException
        Advice->>Repo: save(Auditoria intento rechazado)
        Advice-->>React: HTTP 403 ACCESO_DENEGADO
    else Rol CAJA autorizado
        Ctrl->>Svc: confirmarPedido(key, usuarioId, rol, request)
        Svc->>Repo: findClaveIdempotencia(usuarioId, key)
        Repo->>DB: SELECT FROM clave_idempotencia

        alt 200 OK - Reintento Idempotente RNF-010
            DB-->>Svc: Clave existente
            Svc-->>Ctrl: PedidoConfirmadoResponse original
            Ctrl-->>React: HTTP 200 OK (Sin doble cobro ni doble reserva)
        else Nueva clave de idempotencia
            Svc->>Strat: validarYCalcularVuelto(pago, total)
            alt 422 Unprocessable Entity - Efectivo menor o Nequi no verificado RN-008
                Strat-->>Advice: ReglaNegocioException
                Advice-->>React: HTTP 422 TRANSFERENCIA_NO_VERIFICADA
            else Pago valido
                Svc->>Repo: findAndLockByJornadaIdAndInsumoIds (PESSIMISTIC_WRITE)
                Repo->>DB: SELECT FROM inventario_jornada ORDER BY insumo_id ASC FOR UPDATE
                alt 409 Conflict - Stock agotado por concurrencia RNF-004
                    Svc->>DB: ROLLBACK TRANSACTION
                    Svc-->>Advice: ConcurrenciaInventarioException
                    Advice-->>React: HTTP 409 CONFLICTO_INVENTARIO_CONCURRENTE
                else 201 Created - Reserva atomica exitosa RN-016
                    Svc->>Repo: UPDATE inventario_jornada + INSERT pedido, pago, reserva
                    Repo->>DB: COMMIT (Pedido + Pago + Reserva ACTIVA + Auditoria)
                    Svc-->>Ctrl: PedidoConfirmadoResponse
                    Ctrl-->>React: HTTP 201 Created (Comprobante ID + Nombre)
                end
            end
        end
    end
```

---

## 2.3 Diagrama de Despliegue (UML Deployment Diagram)

```mermaid
flowchart TB
    subgraph Red_Restaurante["1. Red Local del Restaurante - Los Patios"]
        POS["Terminal Caja y Propietario<br/>PC Tactil - Chromium HTTPS"]
        KDS["Pantalla Cocina KDS<br/>Modo Kiosco Solo Lectura RN-021"]
        DESP["Tablet Despacho y Movil Domiciliario<br/>Recepcion, llamado y ruta"]
    end

    subgraph Servidor_Produccion["2. Servidor Linux Ubuntu 24.04 LTS - Docker Engine 27+"]
        NGINX["Docker Container: reverse-proxy-nginx<br/>Nginx 1.27 Alpine | TLS 1.3 HTTPS :443<br/>Sirve build estatico React SPA"]
        BOOT["Docker Container: backend-api<br/>Temurin JRE 17 | Spring Boot 3.4.5 :8080<br/>JVM Flags: -Xms256m -Xmx512m RNF-013"]
        PG[("Docker Container: postgres-db<br/>PostgreSQL 15 Alpine :5432 Red Privada<br/>18 ENUMs, 30 Tablas, Triggers y WAL RPO=0")]
        VOL[("Docker Volumes: pgdata + pg_wal_archive<br/>Snapshots diarios y retencion >= 2 anos RNF-018")]
    end

    POS -->|"HTTPS 443"| NGINX
    KDS -->|"HTTPS 443"| NGINX
    DESP -->|"HTTPS 443"| NGINX
    NGINX -->|"HTTP 8080 /api/v1"| BOOT
    BOOT -->|"JDBC TCP 5432"| PG
    PG --> VOL
```
