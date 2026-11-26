package co.elchupacabras.tickets.pedidos.application;

import co.elchupacabras.tickets.catalogo.infrastructure.CategoriaProductoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.InsumoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.ProductoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.UnidadMedidaJpaEntity;
import co.elchupacabras.tickets.domicilios.infrastructure.DomicilioJpaRepository;
import co.elchupacabras.tickets.identidad.infrastructure.SedeJpaEntity;
import co.elchupacabras.tickets.identidad.infrastructure.UsuarioJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.InventarioJornadaJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.InventarioJornadaJpaRepository;
import co.elchupacabras.tickets.inventario.infrastructure.JornadaJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.MovimientoInventarioJpaRepository;
import co.elchupacabras.tickets.inventario.infrastructure.ReservaInventarioJpaRepository;
import co.elchupacabras.tickets.pedidos.domain.CambioEstadoPedidoEvent;
import co.elchupacabras.tickets.pedidos.domain.EfectivoPagoStrategy;
import co.elchupacabras.tickets.pedidos.domain.TransferenciaNequiPagoStrategy;
import co.elchupacabras.tickets.pedidos.infrastructure.ClaveIdempotenciaJpaRepository;
import co.elchupacabras.tickets.pedidos.infrastructure.PagoJpaRepository;
import co.elchupacabras.tickets.pedidos.infrastructure.PedidoJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.PedidoJpaRepository;
import co.elchupacabras.tickets.pedidos.infrastructure.SolicitudJpaRepository;
import co.elchupacabras.tickets.pedidos.web.PedidoConfirmarRequest;
import co.elchupacabras.tickets.pedidos.web.SolicitudValidarRequest;
import co.elchupacabras.tickets.shared.domain.AccesoDenegadoDominioException;
import co.elchupacabras.tickets.shared.domain.CanalSolicitud;
import co.elchupacabras.tickets.shared.domain.ConcurrenciaInventarioException;
import co.elchupacabras.tickets.shared.domain.EstadoPedido;
import co.elchupacabras.tickets.shared.domain.MedioPago;
import co.elchupacabras.tickets.shared.domain.ModalidadPedido;
import co.elchupacabras.tickets.shared.domain.ReglaNegocioException;
import co.elchupacabras.tickets.shared.domain.TipoInsumo;
import co.elchupacabras.tickets.shared.domain.TipoProducto;
import co.elchupacabras.tickets.shared.infrastructure.AuditoriaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 1. Pruebas Unitarias sobre el Servicio Principal (JUnit 5 + Mockito)
 * Siguiendo estrictamente el patrón AAA (Arrange - Act - Assert).
 */
@ExtendWith(MockitoExtension.class)
class ConfirmadorPedidoServiceTest {

    @Mock
    private ValidadorDisponibilidadService validadorDisponibilidadService;
    @Mock
    private ClaveIdempotenciaJpaRepository idempotenciaRepository;
    @Mock
    private InventarioJornadaJpaRepository inventarioRepository;
    @Mock
    private ReservaInventarioJpaRepository reservaRepository;
    @Mock
    private MovimientoInventarioJpaRepository movimientoRepository;
    @Mock
    private SolicitudJpaRepository solicitudRepository;
    @Mock
    private PedidoJpaRepository pedidoRepository;
    @Mock
    private PagoJpaRepository pagoRepository;
    @Mock
    private DomicilioJpaRepository domicilioRepository;
    @Mock
    private AuditoriaJpaRepository auditoriaRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ConfirmadorPedidoService confirmadorPedidoService;

    private JornadaJpaEntity jornadaActiva;
    private ProductoJpaEntity tacoPastor;
    private InsumoJpaEntity carnePastor;
    private InventarioJornadaJpaEntity inventarioCarne;

    @BeforeEach
    void setUp() {
        confirmadorPedidoService = new ConfirmadorPedidoService(
                validadorDisponibilidadService,
                List.of(new EfectivoPagoStrategy(), new TransferenciaNequiPagoStrategy()),
                idempotenciaRepository,
                inventarioRepository,
                reservaRepository,
                movimientoRepository,
                solicitudRepository,
                pedidoRepository,
                pagoRepository,
                domicilioRepository,
                auditoriaRepository,
                eventPublisher
        );

        SedeJpaEntity sede = new SedeJpaEntity("El Chupacabras Los Patios", "Los Patios", "Norte de Santander");
        ReflectionTestUtils.setField(sede, "id", 1L);

        UsuarioJpaEntity cajero = new UsuarioJpaEntity("caja01", "Carlos Cajero", "$2a$10$hash");
        ReflectionTestUtils.setField(cajero, "id", 10L);

        jornadaActiva = new JornadaJpaEntity(sede, LocalDate.of(2026, 11, 26), cajero);
        ReflectionTestUtils.setField(jornadaActiva, "id", 100L);

        CategoriaProductoJpaEntity categoria = new CategoriaProductoJpaEntity("Tacos", "Tacos mexicanos");
        ReflectionTestUtils.setField(categoria, "id", 1L);

        tacoPastor = new ProductoJpaEntity(
                categoria,
                "TAC-PAST-01",
                "Taco al Pastor",
                TipoProducto.INDIVIDUAL,
                new BigDecimal("12000.00")
        );
        ReflectionTestUtils.setField(tacoPastor, "id", 50L);

        UnidadMedidaJpaEntity unidadGr = new UnidadMedidaJpaEntity("GR", "Gramos", "MASA", BigDecimal.ONE);
        ReflectionTestUtils.setField(unidadGr, "id", 1L);

        carnePastor = new InsumoJpaEntity(
                unidadGr,
                "INS-CARNE-01",
                "Carne al Pastor",
                TipoInsumo.INGREDIENTE,
                new BigDecimal("100.000")
        );
        ReflectionTestUtils.setField(carnePastor, "id", 200L);

        inventarioCarne = new InventarioJornadaJpaEntity(jornadaActiva, carnePastor, new BigDecimal("500.000"));
        ReflectionTestUtils.setField(inventarioCarne, "id", 300L);
    }

    @Test
    @DisplayName("1. Camino feliz (201) + Publicación de eventos: Crea pedido PAGADO_REGISTRADO, calcula vuelto, reserva stock y publica evento 1 vez")
    void confirmarPedido_caminoFeliz_creaPedidoCalculaVueltoReservaStockYPublicaEvento() {
        // Arrange
        String idempotencyKey = "idem-key-001";
        SolicitudValidarRequest solicitudReq = new SolicitudValidarRequest(
                100L,
                CanalSolicitud.PRESENCIAL,
                ModalidadPedido.PARA_LLEVAR,
                "Carlos Soto",
                null,
                null,
                List.of(new SolicitudValidarRequest.ItemSolicitudRequest(50L, 2, "Sin cebolla"))
        );
        PedidoConfirmarRequest request = new PedidoConfirmarRequest(
                solicitudReq,
                new PedidoConfirmarRequest.PagoConfirmarRequest(
                        MedioPago.EFECTIVO,
                        new BigDecimal("30000.00"),
                        null,
                        null
                )
        );

        ValidadorDisponibilidadService.ResultadoCotizacionYReceta cotizacion =
                new ValidadorDisponibilidadService.ResultadoCotizacionYReceta(
                        jornadaActiva,
                        null,
                        Map.of(50L, tacoPastor),
                        Map.of(200L, new BigDecimal("300.000")),
                        new BigDecimal("24000.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("24000.00")
                );

        when(idempotenciaRepository.findByUsuarioIdAndOperacionAndClave(10L, "CONFIRMAR_PEDIDO", idempotencyKey))
                .thenReturn(Optional.empty());
        when(validadorDisponibilidadService.calcularRequerimientosYValidarReglas(solicitudReq))
                .thenReturn(cotizacion);
        when(inventarioRepository.findAndLockByJornadaIdAndInsumoIds(eq(100L), anyCollection()))
                .thenReturn(List.of(inventarioCarne));
        when(solicitudRepository.save(any())).thenAnswer(inv -> {
            var sol = inv.getArgument(0);
            ReflectionTestUtils.setField(sol, "id", 700L);
            return sol;
        });
        when(pedidoRepository.save(any(PedidoJpaEntity.class))).thenAnswer(inv -> {
            PedidoJpaEntity p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 900L);
            return p;
        });

        // Act
        ConfirmadorPedidoService.ResultadoConfirmacion resultado =
                confirmadorPedidoService.confirmarPedido(idempotencyKey, 10L, "CAJA", request);

        // Assert
        assertThat(resultado.reintentoIdempotente()).isFalse();
        assertThat(resultado.response().id()).isEqualTo(900L);
        assertThat(resultado.response().pedidoId()).isEqualTo("PED-20261126-0001");
        assertThat(resultado.response().estado()).isEqualTo(EstadoPedido.PAGADO_REGISTRADO);
        assertThat(resultado.response().total()).isEqualByComparingTo("24000.00");
        assertThat(resultado.response().vuelto()).isEqualByComparingTo("6000.00");
        assertThat(inventarioCarne.getCantidadDisponible()).isEqualByComparingTo("200.000");
        assertThat(inventarioCarne.getCantidadReservada()).isEqualByComparingTo("300.000");

        verify(pedidoRepository, times(1)).save(any(PedidoJpaEntity.class));
        verify(reservaRepository, times(1)).save(any());
        verify(eventPublisher, times(1)).publishEvent(any(CambioEstadoPedidoEvent.class));
    }

    @Test
    @DisplayName("2. Regla de negocio / Verificación manual Nequi (RN-008): Lanza ReglaNegocioException (422) sin llamar a pedidoRepository.save() ni publicar evento")
    void confirmarPedido_transferenciaNequiNoVerificada_lanzaExcepcion422SinGuardarNiPublicarEvento() {
        // Arrange
        String idempotencyKey = "idem-key-nequi-422";
        SolicitudValidarRequest solicitudReq = new SolicitudValidarRequest(
                100L,
                CanalSolicitud.WHATSAPP,
                ModalidadPedido.RECOGER,
                "Kevin Angarita",
                null,
                null,
                List.of(new SolicitudValidarRequest.ItemSolicitudRequest(50L, 1, null))
        );
        PedidoConfirmarRequest request = new PedidoConfirmarRequest(
                solicitudReq,
                new PedidoConfirmarRequest.PagoConfirmarRequest(
                        MedioPago.TRANSFERENCIA,
                        null,
                        false,
                        "BREB-999"
                )
        );

        ValidadorDisponibilidadService.ResultadoCotizacionYReceta cotizacion =
                new ValidadorDisponibilidadService.ResultadoCotizacionYReceta(
                        jornadaActiva,
                        null,
                        Map.of(50L, tacoPastor),
                        Map.of(200L, new BigDecimal("150.000")),
                        new BigDecimal("12000.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("12000.00")
                );

        when(idempotenciaRepository.findByUsuarioIdAndOperacionAndClave(10L, "CONFIRMAR_PEDIDO", idempotencyKey))
                .thenReturn(Optional.empty());
        when(validadorDisponibilidadService.calcularRequerimientosYValidarReglas(solicitudReq))
                .thenReturn(cotizacion);

        // Act & Assert
        assertThatThrownBy(() -> confirmadorPedidoService.confirmarPedido(idempotencyKey, 10L, "CAJA", request))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo")
                .isEqualTo("TRANSFERENCIA_NO_VERIFICADA");

        verify(pedidoRepository, never()).save(any());
        verify(reservaRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("3. Valores límite en pago efectivo (RN-003): Rechaza monto $23.999,99 (422) y acepta monto exacto $24.000,00 con vuelto $0,00")
    void confirmarPedido_valoresLimitePagoEfectivo_rechazaPorUnCentavoYAceptaMontoExacto() {
        // Arrange
        SolicitudValidarRequest solicitudReq = new SolicitudValidarRequest(
                100L,
                CanalSolicitud.PRESENCIAL,
                ModalidadPedido.CONSUMO_LOCAL,
                "Juan Jaimes",
                null,
                null,
                List.of(new SolicitudValidarRequest.ItemSolicitudRequest(50L, 2, null))
        );

        ValidadorDisponibilidadService.ResultadoCotizacionYReceta cotizacion =
                new ValidadorDisponibilidadService.ResultadoCotizacionYReceta(
                        jornadaActiva,
                        null,
                        Map.of(50L, tacoPastor),
                        Map.of(200L, new BigDecimal("300.000")),
                        new BigDecimal("24000.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("24000.00")
                );

        when(idempotenciaRepository.findByUsuarioIdAndOperacionAndClave(eq(10L), eq("CONFIRMAR_PEDIDO"), any()))
                .thenReturn(Optional.empty());
        when(validadorDisponibilidadService.calcularRequerimientosYValidarReglas(solicitudReq))
                .thenReturn(cotizacion);

        PedidoConfirmarRequest requestInferiorAlLimite = new PedidoConfirmarRequest(
                solicitudReq,
                new PedidoConfirmarRequest.PagoConfirmarRequest(
                        MedioPago.EFECTIVO,
                        new BigDecimal("23999.99"),
                        null,
                        null
                )
        );

        // Act & Assert (Límite inferior: 23999.99 vs 24000.00 -> 422 sin guardar)
        assertThatThrownBy(() -> confirmadorPedidoService.confirmarPedido("idem-limite-1", 10L, "CAJA", requestInferiorAlLimite))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo")
                .isEqualTo("EFECTIVO_INSUFICIENTE");

        verify(pedidoRepository, never()).save(any());

        // Arrange (Límite exacto: 24000.00 vs 24000.00 -> 201 con vuelto 0.00)
        when(inventarioRepository.findAndLockByJornadaIdAndInsumoIds(eq(100L), anyCollection()))
                .thenReturn(List.of(inventarioCarne));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(PedidoJpaEntity.class))).thenAnswer(inv -> {
            PedidoJpaEntity p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 901L);
            return p;
        });

        PedidoConfirmarRequest requestLimiteExacto = new PedidoConfirmarRequest(
                solicitudReq,
                new PedidoConfirmarRequest.PagoConfirmarRequest(
                        MedioPago.EFECTIVO,
                        new BigDecimal("24000.00"),
                        null,
                        null
                )
        );

        // Act
        ConfirmadorPedidoService.ResultadoConfirmacion resultadoExacto =
                confirmadorPedidoService.confirmarPedido("idem-limite-2", 10L, "CAJA", requestLimiteExacto);

        // Assert
        assertThat(resultadoExacto.response().vuelto()).isEqualByComparingTo("0.00");
        verify(pedidoRepository, times(1)).save(any(PedidoJpaEntity.class));
    }

    @Test
    @DisplayName("4. Choque de disponibilidad por concurrencia (RNF-004): Lanza ConcurrenciaInventarioException (409) sin guardar pedido ni publicar evento")
    void confirmarPedido_choqueDisponibilidadConcurrente_lanzaConcurrenciaException409() {
        // Arrange
        String idempotencyKey = "idem-key-409";
        SolicitudValidarRequest solicitudReq = new SolicitudValidarRequest(
                100L,
                CanalSolicitud.PRESENCIAL,
                ModalidadPedido.PARA_LLEVAR,
                "Cliente Concurrente",
                null,
                null,
                List.of(new SolicitudValidarRequest.ItemSolicitudRequest(50L, 2, null))
        );
        PedidoConfirmarRequest request = new PedidoConfirmarRequest(
                solicitudReq,
                new PedidoConfirmarRequest.PagoConfirmarRequest(
                        MedioPago.EFECTIVO,
                        new BigDecimal("25000.00"),
                        null,
                        null
                )
        );

        // La cotización inicial requería 300g, pero al adquirir el lock PESSIMISTIC_WRITE solo quedan 150g disponibles
        InventarioJornadaJpaEntity inventarioAgotadoPorOtroHilo =
                new InventarioJornadaJpaEntity(jornadaActiva, carnePastor, new BigDecimal("150.000"));
        ReflectionTestUtils.setField(inventarioAgotadoPorOtroHilo, "id", 300L);

        ValidadorDisponibilidadService.ResultadoCotizacionYReceta cotizacion =
                new ValidadorDisponibilidadService.ResultadoCotizacionYReceta(
                        jornadaActiva,
                        null,
                        Map.of(50L, tacoPastor),
                        Map.of(200L, new BigDecimal("300.000")),
                        new BigDecimal("24000.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("24000.00")
                );

        when(idempotenciaRepository.findByUsuarioIdAndOperacionAndClave(10L, "CONFIRMAR_PEDIDO", idempotencyKey))
                .thenReturn(Optional.empty());
        when(validadorDisponibilidadService.calcularRequerimientosYValidarReglas(solicitudReq))
                .thenReturn(cotizacion);
        when(inventarioRepository.findAndLockByJornadaIdAndInsumoIds(eq(100L), anyCollection()))
                .thenReturn(List.of(inventarioAgotadoPorOtroHilo));

        // Act & Assert
        assertThatThrownBy(() -> confirmadorPedidoService.confirmarPedido(idempotencyKey, 10L, "CAJA", request))
                .isInstanceOf(ConcurrenciaInventarioException.class)
                .extracting("codigo")
                .isEqualTo("CONFLICTO_INVENTARIO_CONCURRENTE");

        verify(pedidoRepository, never()).save(any());
        verify(reservaRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("5. Recurso inexistente y aislamiento RBAC (RN-021): Rechaza pedido inexistente y audita intento de Cocina de cambiar estado (403)")
    void cambiarEstadoPedido_pedidoInexistenteORolCocina_lanzaExcepcionYNoPublicaEvento() {
        // Arrange (Pedido inexistente)
        when(pedidoRepository.findById(9999L)).thenReturn(Optional.empty());

        // Act & Assert (Recurso inexistente)
        assertThatThrownBy(() -> confirmadorPedidoService.cambiarEstadoPedido(
                9999L,
                EstadoPedido.EN_PREPARACION,
                10L,
                "CAJA",
                "Intento sobre ID inexistente"
        ))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo")
                .isEqualTo("PEDIDO_NO_ENCONTRADO");

        // Act & Assert (Rol COCINA intenta mutar estado -> 403 Forbidden y auditado)
        assertThatThrownBy(() -> confirmadorPedidoService.cambiarEstadoPedido(
                900L,
                EstadoPedido.LISTO_PARA_DESPACHO,
                20L,
                "COCINA",
                "Cocina intenta marcar listo"
        ))
                .isInstanceOf(AccesoDenegadoDominioException.class)
                .extracting("codigo")
                .isEqualTo("COCINA_SOLO_LECTURA");

        verify(auditoriaRepository, times(1)).save(any());
        verify(pedidoRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
