package co.elchupacabras.tickets.pedidos.application;

import co.elchupacabras.tickets.catalogo.infrastructure.ProductoJpaEntity;
import co.elchupacabras.tickets.domicilios.infrastructure.DomicilioJpaEntity;
import co.elchupacabras.tickets.domicilios.infrastructure.DomicilioJpaRepository;
import co.elchupacabras.tickets.inventario.infrastructure.InventarioJornadaJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.InventarioJornadaJpaRepository;
import co.elchupacabras.tickets.inventario.infrastructure.MovimientoInventarioJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.MovimientoInventarioJpaRepository;
import co.elchupacabras.tickets.inventario.infrastructure.ReservaInventarioJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.ReservaInventarioJpaRepository;
import co.elchupacabras.tickets.pedidos.domain.CambioEstadoPedidoEvent;
import co.elchupacabras.tickets.pedidos.domain.DatosPagoComando;
import co.elchupacabras.tickets.pedidos.domain.ValidadorPagoStrategy;
import co.elchupacabras.tickets.pedidos.infrastructure.ClaveIdempotenciaJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.ClaveIdempotenciaJpaRepository;
import co.elchupacabras.tickets.pedidos.infrastructure.PagoJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.PagoJpaRepository;
import co.elchupacabras.tickets.pedidos.infrastructure.PedidoDetalleJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.PedidoJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.PedidoJpaRepository;
import co.elchupacabras.tickets.pedidos.infrastructure.SolicitudDetalleJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.SolicitudJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.SolicitudJpaRepository;
import co.elchupacabras.tickets.pedidos.web.PedidoConfirmadoResponse;
import co.elchupacabras.tickets.pedidos.web.PedidoConfirmarRequest;
import co.elchupacabras.tickets.pedidos.web.SolicitudValidarRequest;
import co.elchupacabras.tickets.shared.domain.AccesoDenegadoDominioException;
import co.elchupacabras.tickets.shared.domain.ConcurrenciaInventarioException;
import co.elchupacabras.tickets.shared.domain.EstadoPedido;
import co.elchupacabras.tickets.shared.domain.EstadoReserva;
import co.elchupacabras.tickets.shared.domain.MedioPago;
import co.elchupacabras.tickets.shared.domain.ModalidadPedido;
import co.elchupacabras.tickets.shared.domain.ReglaNegocioException;
import co.elchupacabras.tickets.shared.domain.TipoMovimientoInventario;
import co.elchupacabras.tickets.shared.domain.TipoPago;
import co.elchupacabras.tickets.shared.infrastructure.AuditoriaJpaEntity;
import co.elchupacabras.tickets.shared.infrastructure.AuditoriaJpaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio transaccional principal (Taller 3 / T3-01, HU-001, RN-016, RNF-004, RNF-010):
 * Aplica SOLID (DIP + OCP + SRP), inyección por constructor, Patrón Strategy para pagos
 * y Patrón Observer (ApplicationEventPublisher) para historial y auditoría inmutable.
 */
@Service
public class ConfirmadorPedidoService {

    private static final String OPERACION_CONFIRMAR = "CONFIRMAR_PEDIDO";

    private final ValidadorDisponibilidadService validadorDisponibilidadService;
    private final Map<MedioPago, ValidadorPagoStrategy> estrategiasPago;
    private final ClaveIdempotenciaJpaRepository idempotenciaRepository;
    private final InventarioJornadaJpaRepository inventarioRepository;
    private final ReservaInventarioJpaRepository reservaRepository;
    private final MovimientoInventarioJpaRepository movimientoRepository;
    private final SolicitudJpaRepository solicitudRepository;
    private final PedidoJpaRepository pedidoRepository;
    private final PagoJpaRepository pagoRepository;
    private final DomicilioJpaRepository domicilioRepository;
    private final AuditoriaJpaRepository auditoriaRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final AtomicInteger secuenciaTicket = new AtomicInteger(1);

    public ConfirmadorPedidoService(
            ValidadorDisponibilidadService validadorDisponibilidadService,
            List<ValidadorPagoStrategy> estrategiasPagoList,
            ClaveIdempotenciaJpaRepository idempotenciaRepository,
            InventarioJornadaJpaRepository inventarioRepository,
            ReservaInventarioJpaRepository reservaRepository,
            MovimientoInventarioJpaRepository movimientoRepository,
            SolicitudJpaRepository solicitudRepository,
            PedidoJpaRepository pedidoRepository,
            PagoJpaRepository pagoRepository,
            DomicilioJpaRepository domicilioRepository,
            AuditoriaJpaRepository auditoriaRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.validadorDisponibilidadService = validadorDisponibilidadService;
        this.estrategiasPago = estrategiasPagoList.stream()
                .collect(Collectors.toMap(ValidadorPagoStrategy::medioSoportado, Function.identity()));
        this.idempotenciaRepository = idempotenciaRepository;
        this.inventarioRepository = inventarioRepository;
        this.reservaRepository = reservaRepository;
        this.movimientoRepository = movimientoRepository;
        this.solicitudRepository = solicitudRepository;
        this.pedidoRepository = pedidoRepository;
        this.pagoRepository = pagoRepository;
        this.domicilioRepository = domicilioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.eventPublisher = eventPublisher;
    }

    public record ResultadoConfirmacion(
            boolean reintentoIdempotente,
            PedidoConfirmadoResponse response
    ) {
    }

    @Transactional
    public ResultadoConfirmacion confirmarPedido(
            String idempotencyKey,
            Long usuarioId,
            String rolActor,
            PedidoConfirmarRequest request
    ) {
        if (!"CAJA".equalsIgnoreCase(rolActor) && !"PROPIETARIO".equalsIgnoreCase(rolActor)) {
            auditoriaRepository.save(new AuditoriaJpaEntity(
                    usuarioId,
                    "INTENTO_NO_AUTORIZADO_CONFIRMAR_PEDIDO",
                    "PEDIDO",
                    "N/A",
                    null,
                    "{\"rol\":\"" + rolActor + "\"}"
            ));
            throw new AccesoDenegadoDominioException(
                    "ROL_NO_AUTORIZADO_CAJA",
                    "Solo el rol CAJA o PROPIETARIO puede confirmar pagos y crear pedidos (RN-020, RNF-005)"
            );
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("El encabezado Idempotency-Key es obligatorio (RNF-010)");
        }

        Optional<ClaveIdempotenciaJpaEntity> claveExistente = idempotenciaRepository
                .findByUsuarioIdAndOperacionAndClave(usuarioId, OPERACION_CONFIRMAR, idempotencyKey);

        if (claveExistente.isPresent()) {
            PedidoJpaEntity pedidoPrevio = pedidoRepository.findById(claveExistente.get().getPedidoId())
                    .orElseThrow(() -> new ReglaNegocioException("PEDIDO_IDEMPOTENTE_NO_ENCONTRADO", "El pedido asociado a la clave no existe"));
            BigDecimal vueltoPrevio = pagoRepository.findByPedidoId(pedidoPrevio.getId()).stream()
                    .findFirst()
                    .map(PagoJpaEntity::getVuelto)
                    .orElse(BigDecimal.ZERO);

            return new ResultadoConfirmacion(
                    true,
                    mapToResponse(pedidoPrevio, vueltoPrevio)
            );
        }

        ValidadorDisponibilidadService.ResultadoCotizacionYReceta cotizacion =
                validadorDisponibilidadService.calcularRequerimientosYValidarReglas(request.solicitud());

        ValidadorPagoStrategy strategy = estrategiasPago.get(request.pago().medio());
        if (strategy == null) {
            throw new ReglaNegocioException("MEDIO_PAGO_NO_SOPORTADO", "El medio de pago no es admitido (RN-003)");
        }

        DatosPagoComando datosPago = new DatosPagoComando(
                request.pago().medio(),
                request.pago().montoRecibido(),
                request.pago().transferenciaVerificada(),
                request.pago().referenciaExterna()
        );
        BigDecimal vuelto = strategy.validarYCalcularVuelto(datosPago, cotizacion.total());

        // Bloqueo pesimista ordenado por insumo_id ASC (RNF-004, ADR-002)
        List<InventarioJornadaJpaEntity> inventariosBloqueados = inventarioRepository
                .findAndLockByJornadaIdAndInsumoIds(cotizacion.jornada().getId(), cotizacion.requerimientosPorInsumo().keySet());

        Map<Long, InventarioJornadaJpaEntity> bloqueadosPorInsumo = inventariosBloqueados.stream()
                .collect(Collectors.toMap(ij -> ij.getInsumo().getId(), Function.identity()));

        for (Map.Entry<Long, BigDecimal> reqEntry : cotizacion.requerimientosPorInsumo().entrySet()) {
            InventarioJornadaJpaEntity inv = bloqueadosPorInsumo.get(reqEntry.getKey());
            if (inv == null || !inv.tieneDisponibilidad(reqEntry.getValue())) {
                throw new ConcurrenciaInventarioException(
                        "CONFLICTO_INVENTARIO_CONCURRENTE",
                        "Otro pedido consumió el stock disponible entre la validación y la confirmación (RNF-004)"
                );
            }
        }

        // 1. Persistir Solicitud en estado CONVERTIDA
        SolicitudJpaEntity solicitud = new SolicitudJpaEntity(
                cotizacion.jornada(),
                usuarioId,
                request.solicitud().canal(),
                request.solicitud().modalidad(),
                request.solicitud().nombreCliente(),
                cotizacion.zonaDomicilio(),
                request.solicitud().direccionTexto()
        );
        for (SolicitudValidarRequest.ItemSolicitudRequest item : request.solicitud().items()) {
            ProductoJpaEntity prod = cotizacion.productosPorId().get(item.productoId());
            solicitud.agregarDetalle(new SolicitudDetalleJpaEntity(solicitud, prod, item.cantidad(), item.preferencias()));
        }
        solicitud.marcarConvertida();
        solicitud = solicitudRepository.save(solicitud);

        // 2. Crear Pedido en estado PAGADO_REGISTRADO con instantáneas comerciales
        String codigoPublico = generarCodigoPublico(cotizacion.jornada());
        PedidoJpaEntity pedido = new PedidoJpaEntity(solicitud, codigoPublico, usuarioId);
        for (SolicitudValidarRequest.ItemSolicitudRequest item : request.solicitud().items()) {
            ProductoJpaEntity prod = cotizacion.productosPorId().get(item.productoId());
            pedido.agregarDetalle(new PedidoDetalleJpaEntity(
                    pedido,
                    prod.getId(),
                    prod.getCodigo(),
                    prod.getNombre(),
                    item.cantidad(),
                    prod.getPrecio(),
                    item.preferencias()
            ));
        }
        pedido = pedidoRepository.save(pedido);

        // 3. Registrar Pago principal
        PagoJpaEntity pago = new PagoJpaEntity(
                pedido,
                usuarioId,
                TipoPago.PEDIDO,
                request.pago().medio(),
                cotizacion.total(),
                request.pago().montoRecibido(),
                vuelto,
                request.pago().transferenciaVerificada(),
                request.pago().referenciaExterna()
        );
        pagoRepository.save(pago);

        // 4. Si es DOMICILIO, crear registro en tabla domicilio con snapshot de zona y tarifa
        if (pedido.getModalidad() == ModalidadPedido.DOMICILIO && cotizacion.zonaDomicilio() != null) {
            DomicilioJpaEntity domicilio = new DomicilioJpaEntity(
                    pedido.getId(),
                    cotizacion.zonaDomicilio(),
                    request.solicitud().direccionTexto()
            );
            domicilioRepository.save(domicilio);
        }

        // 5. Descontar disponible, aumentar reservada y persistir ReservaInventario ACTIVA (RN-016)
        for (Map.Entry<Long, BigDecimal> reqEntry : cotizacion.requerimientosPorInsumo().entrySet()) {
            InventarioJornadaJpaEntity inv = bloqueadosPorInsumo.get(reqEntry.getKey());
            inv.reservar(reqEntry.getValue());
            inventarioRepository.save(inv);

            ReservaInventarioJpaEntity reserva = new ReservaInventarioJpaEntity(
                    pedido.getId(),
                    inv,
                    reqEntry.getValue()
            );
            reservaRepository.save(reserva);
        }

        // 6. Guardar clave de idempotencia (RNF-010)
        PedidoConfirmadoResponse response = mapToResponse(pedido, vuelto);
        ClaveIdempotenciaJpaEntity registroIdempotencia = new ClaveIdempotenciaJpaEntity(
                usuarioId,
                OPERACION_CONFIRMAR,
                idempotencyKey,
                pedido.getId(),
                String.valueOf(request.hashCode()),
                (short) 201,
                "{\"pedidoId\":\"" + response.pedidoId() + "\"}",
                OffsetDateTime.now().plusHours(24)
        );
        idempotenciaRepository.save(registroIdempotencia);

        // 7. Publicar evento de dominio para historial_estado_pedido y auditoria (Observer Pattern)
        eventPublisher.publishEvent(new CambioEstadoPedidoEvent(
                pedido.getId(),
                pedido.getCodigoPublico(),
                null,
                EstadoPedido.PAGADO_REGISTRADO,
                usuarioId,
                "USUARIO",
                "Pedido confirmado con inventario reservado"
        ));

        return new ResultadoConfirmacion(false, response);
    }

    /**
     * Ejecuta transiciones del ciclo de vida del pedido (RN-016, RN-017, RN-021, RN-026, RN-027, RN-034).
     * Consume reservas cuando Despacho recibe de Cocina; libera reservas si se cancela antes de preparar.
     */
    @Transactional
    public PedidoConfirmadoResponse cambiarEstadoPedido(
            Long pedidoId,
            EstadoPedido nuevoEstado,
            Long usuarioId,
            String rolActor,
            String observacion
    ) {
        if ("COCINA".equalsIgnoreCase(rolActor)) {
            auditoriaRepository.save(new AuditoriaJpaEntity(
                    usuarioId,
                    "INTENTO_COCINA_CAMBIAR_ESTADO",
                    "PEDIDO",
                    String.valueOf(pedidoId),
                    null,
                    "{\"estadoSolicitado\":\"" + nuevoEstado + "\"}"
            ));
            throw new AccesoDenegadoDominioException(
                    "COCINA_SOLO_LECTURA",
                    "Cocina solo consulta y prepara pedidos; no registra cambios de estado (RN-021, RN-027)"
            );
        }

        PedidoJpaEntity pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ReglaNegocioException("PEDIDO_NO_ENCONTRADO", "El pedido no existe"));

        EstadoPedido estadoAnterior = pedido.getEstado();
        pedido.transicionarEstado(nuevoEstado, rolActor);
        pedido = pedidoRepository.save(pedido);

        OffsetDateTime ahora = OffsetDateTime.now();

        // RN-016 y RN-027: Al pasar a LISTO_PARA_DESPACHO, las reservas activas se consumen
        if (nuevoEstado == EstadoPedido.LISTO_PARA_DESPACHO) {
            List<ReservaInventarioJpaEntity> reservas = reservaRepository.findByPedidoIdWithInventario(pedido.getId());
            for (ReservaInventarioJpaEntity reserva : reservas) {
                if (reserva.getEstado() == EstadoReserva.ACTIVA) {
                    reserva.marcarConsumida(ahora);
                    InventarioJornadaJpaEntity inv = reserva.getInventarioJornada();
                    inv.consumirReserva(reserva.getCantidad());
                    inventarioRepository.save(inv);
                    reservaRepository.save(reserva);

                    movimientoRepository.save(new MovimientoInventarioJpaEntity(
                            inv,
                            reserva.getId(),
                            usuarioId,
                            TipoMovimientoInventario.CONSUMO,
                            reserva.getCantidad(),
                            "Consumo confirmado al recibir pedido " + pedido.getCodigoPublico() + " en Despacho"
                    ));
                }
            }
        }

        // RN-017 y RN-034: Si se cancela antes de iniciar preparación, liberar reservas activas
        if (nuevoEstado == EstadoPedido.CANCELADO_REEMBOLSO_PENDIENTE && estadoAnterior == EstadoPedido.PAGADO_REGISTRADO) {
            List<ReservaInventarioJpaEntity> reservas = reservaRepository.findByPedidoIdWithInventario(pedido.getId());
            for (ReservaInventarioJpaEntity reserva : reservas) {
                if (reserva.getEstado() == EstadoReserva.ACTIVA) {
                    reserva.marcarLiberada(ahora);
                    InventarioJornadaJpaEntity inv = reserva.getInventarioJornada();
                    inv.liberarReserva(reserva.getCantidad());
                    inventarioRepository.save(inv);
                    reservaRepository.save(reserva);

                    movimientoRepository.save(new MovimientoInventarioJpaEntity(
                            inv,
                            reserva.getId(),
                            usuarioId,
                            TipoMovimientoInventario.LIBERACION,
                            reserva.getCantidad(),
                            "Liberación de reserva por cancelación autorizada antes de preparación"
                    ));
                }
            }
        }

        eventPublisher.publishEvent(new CambioEstadoPedidoEvent(
                pedido.getId(),
                pedido.getCodigoPublico(),
                estadoAnterior,
                nuevoEstado,
                usuarioId,
                "USUARIO",
                observacion != null ? observacion : "Cambio de estado a " + nuevoEstado
        ));

        return mapToResponse(pedido, BigDecimal.ZERO);
    }

    private String generarCodigoPublico(co.elchupacabras.tickets.inventario.infrastructure.JornadaJpaEntity jornada) {
        String fecha = jornada.getFechaOperacion().format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("PED-%s-%04d", fecha, secuenciaTicket.getAndIncrement());
    }

    private PedidoConfirmadoResponse mapToResponse(PedidoJpaEntity pedido, BigDecimal vuelto) {
        return new PedidoConfirmadoResponse(
                pedido.getId(),
                pedido.getCodigoPublico(),
                pedido.getNombreCliente(),
                pedido.getEstado(),
                pedido.getSubtotal(),
                pedido.getCostoDomicilio(),
                pedido.getTotal(),
                vuelto,
                pedido.getCreadoEn()
        );
    }
}
