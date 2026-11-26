package co.elchupacabras.tickets.pedidos.application;

import co.elchupacabras.tickets.catalogo.infrastructure.CategoriaProductoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.InsumoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.ProductoInsumoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.ProductoInsumoJpaRepository;
import co.elchupacabras.tickets.catalogo.infrastructure.ProductoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.ProductoJpaRepository;
import co.elchupacabras.tickets.catalogo.infrastructure.UnidadMedidaJpaEntity;
import co.elchupacabras.tickets.domicilios.infrastructure.ZonaDomicilioJpaEntity;
import co.elchupacabras.tickets.domicilios.infrastructure.ZonaDomicilioJpaRepository;
import co.elchupacabras.tickets.identidad.infrastructure.SedeJpaEntity;
import co.elchupacabras.tickets.identidad.infrastructure.UsuarioJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.InventarioJornadaJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.InventarioJornadaJpaRepository;
import co.elchupacabras.tickets.inventario.infrastructure.JornadaJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.JornadaJpaRepository;
import co.elchupacabras.tickets.pedidos.web.SolicitudValidarRequest;
import co.elchupacabras.tickets.pedidos.web.SolicitudValidarResponse;
import co.elchupacabras.tickets.shared.domain.CanalSolicitud;
import co.elchupacabras.tickets.shared.domain.ModalidadPedido;
import co.elchupacabras.tickets.shared.domain.ReglaNegocioException;
import co.elchupacabras.tickets.shared.domain.TipoInsumo;
import co.elchupacabras.tickets.shared.domain.TipoProducto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Pruebas Unitarias sobre ValidadorDisponibilidadService (JUnit 5 + Mockito)
 * Siguiendo el patrón AAA (Arrange - Act - Assert).
 */
@ExtendWith(MockitoExtension.class)
class ValidadorDisponibilidadServiceTest {

    @Mock
    private JornadaJpaRepository jornadaRepository;
    @Mock
    private ProductoJpaRepository productoRepository;
    @Mock
    private ProductoInsumoJpaRepository recetaRepository;
    @Mock
    private InventarioJornadaJpaRepository inventarioRepository;
    @Mock
    private ZonaDomicilioJpaRepository zonaDomicilioRepository;

    private ValidadorDisponibilidadService validadorService;

    private SedeJpaEntity sede;
    private JornadaJpaEntity jornadaAbierta;
    private ProductoJpaEntity tacoPastor;
    private InsumoJpaEntity carnePastor;

    @BeforeEach
    void setUp() {
        validadorService = new ValidadorDisponibilidadService(
                jornadaRepository,
                productoRepository,
                recetaRepository,
                inventarioRepository,
                zonaDomicilioRepository
        );

        sede = new SedeJpaEntity("El Chupacabras", "Los Patios", "Norte de Santander");
        ReflectionTestUtils.setField(sede, "id", 1L);

        UsuarioJpaEntity cajero = new UsuarioJpaEntity("caja01", "Carlos Cajero", "$2a$10$hash");
        ReflectionTestUtils.setField(cajero, "id", 10L);

        jornadaAbierta = new JornadaJpaEntity(sede, LocalDate.of(2026, 11, 26), cajero);
        ReflectionTestUtils.setField(jornadaAbierta, "id", 100L);

        CategoriaProductoJpaEntity cat = new CategoriaProductoJpaEntity("Tacos", "Tacos");
        tacoPastor = new ProductoJpaEntity(cat, "TAC-01", "Taco al Pastor", TipoProducto.INDIVIDUAL, new BigDecimal("12000.00"));
        ReflectionTestUtils.setField(tacoPastor, "id", 50L);

        UnidadMedidaJpaEntity gr = new UnidadMedidaJpaEntity("GR", "Gramos", "MASA", BigDecimal.ONE);
        carnePastor = new InsumoJpaEntity(gr, "INS-01", "Carne al Pastor", TipoInsumo.INGREDIENTE, new BigDecimal("100.000"));
        ReflectionTestUtils.setField(carnePastor, "id", 200L);
    }

    @Test
    @DisplayName("1. Camino feliz domicilio en Los Patios (RN-004, RN-028): Cotiza subtotal + tarifa domicilio sin modificar stock disponible")
    void validarSolicitud_domicilioEnLosPatiosConStockSuficiente_calculaTotalSinReservarStock() {
        // Arrange
        ZonaDomicilioJpaEntity zonaLosPatios = new ZonaDomicilioJpaEntity(
                sede,
                "Patio Antiguo",
                "Los Patios",
                new BigDecimal("4000.00"),
                true
        );
        ReflectionTestUtils.setField(zonaLosPatios, "id", 5L);

        ProductoInsumoJpaEntity lineaReceta = new ProductoInsumoJpaEntity(tacoPastor, carnePastor, new BigDecimal("150.000"));
        InventarioJornadaJpaEntity inventario = new InventarioJornadaJpaEntity(jornadaAbierta, carnePastor, new BigDecimal("300.000"));

        when(jornadaRepository.findById(100L)).thenReturn(Optional.of(jornadaAbierta));
        when(zonaDomicilioRepository.findById(5L)).thenReturn(Optional.of(zonaLosPatios));
        when(productoRepository.findAllById(List.of(50L))).thenReturn(List.of(tacoPastor));
        when(recetaRepository.findRecetasByProductoIds(List.of(50L))).thenReturn(List.of(lineaReceta));
        when(inventarioRepository.findByJornadaIdAndInsumoIds(eq(100L), anyCollection())).thenReturn(List.of(inventario));

        SolicitudValidarRequest request = new SolicitudValidarRequest(
                100L,
                CanalSolicitud.WHATSAPP,
                ModalidadPedido.DOMICILIO,
                "Kevin Angarita",
                5L,
                "Calle 10 # 4-25 Patio Antiguo",
                List.of(new SolicitudValidarRequest.ItemSolicitudRequest(50L, 2, "Con piña"))
        );

        // Act
        SolicitudValidarResponse response = validadorService.validarSolicitud(request);

        // Assert
        assertThat(response.solicitudValida()).isTrue();
        assertThat(response.subtotal()).isEqualByComparingTo("24000.00");
        assertThat(response.costoDomicilio()).isEqualByComparingTo("4000.00");
        assertThat(response.total()).isEqualByComparingTo("28000.00");
        assertThat(inventario.getCantidadDisponible()).isEqualByComparingTo("300.000");
        assertThat(inventario.getCantidadReservada()).isEqualByComparingTo("0.000");
    }

    @Test
    @DisplayName("2. Recurso inexistente o inactivo (422): Rechaza cuando el producto consultado está inactivo o la jornada no existe")
    void validarSolicitud_productoInactivoOJornadaInexistente_lanzaReglaNegocioException() {
        // Arrange (Jornada inexistente)
        when(jornadaRepository.findById(999L)).thenReturn(Optional.empty());
        SolicitudValidarRequest reqJornadaInexistente = new SolicitudValidarRequest(
                999L,
                CanalSolicitud.PRESENCIAL,
                ModalidadPedido.CONSUMO_LOCAL,
                "Carlos",
                null,
                null,
                List.of(new SolicitudValidarRequest.ItemSolicitudRequest(50L, 1, null))
        );

        // Act & Assert
        assertThatThrownBy(() -> validadorService.validarSolicitud(reqJornadaInexistente))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo")
                .isEqualTo("JORNADA_NO_ENCONTRADA");

        // Arrange (Producto inactivo)
        ReflectionTestUtils.setField(tacoPastor, "activo", false);
        when(jornadaRepository.findById(100L)).thenReturn(Optional.of(jornadaAbierta));
        when(productoRepository.findAllById(List.of(50L))).thenReturn(List.of(tacoPastor));

        SolicitudValidarRequest reqProductoInactivo = new SolicitudValidarRequest(
                100L,
                CanalSolicitud.PRESENCIAL,
                ModalidadPedido.CONSUMO_LOCAL,
                "Carlos",
                null,
                null,
                List.of(new SolicitudValidarRequest.ItemSolicitudRequest(50L, 1, null))
        );

        // Act & Assert
        assertThatThrownBy(() -> validadorService.validarSolicitud(reqProductoInactivo))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo")
                .isEqualTo("PRODUCTO_NO_DISPONIBLE");
    }

    @Test
    @DisplayName("3. Cobertura geográfica estricta en Los Patios (RN-028): Rechaza zona fuera de Los Patios con 422 DOMICILIO_FUERA_DE_COBERTURA")
    void validarSolicitud_zonaFueraDeLosPatios_lanzaExcepcionDomicilioFueraDeCobertura() {
        // Arrange
        ZonaDomicilioJpaEntity zonaCucuta = new ZonaDomicilioJpaEntity(
                sede,
                "Caobos",
                "Cúcuta",
                new BigDecimal("8000.00"),
                true
        );
        ReflectionTestUtils.setField(zonaCucuta, "id", 99L);

        when(jornadaRepository.findById(100L)).thenReturn(Optional.of(jornadaAbierta));
        when(zonaDomicilioRepository.findById(99L)).thenReturn(Optional.of(zonaCucuta));

        SolicitudValidarRequest requestFueraCobertura = new SolicitudValidarRequest(
                100L,
                CanalSolicitud.WHATSAPP,
                ModalidadPedido.DOMICILIO,
                "Cliente Externo",
                99L,
                "Av. Libertadores Cúcuta",
                List.of(new SolicitudValidarRequest.ItemSolicitudRequest(50L, 1, null))
        );

        // Act & Assert
        assertThatThrownBy(() -> validadorService.validarSolicitud(requestFueraCobertura))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo")
                .isEqualTo("DOMICILIO_FUERA_DE_COBERTURA");
    }
}
