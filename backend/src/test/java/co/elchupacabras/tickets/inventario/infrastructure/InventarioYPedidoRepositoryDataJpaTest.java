package co.elchupacabras.tickets.inventario.infrastructure;

import co.elchupacabras.tickets.catalogo.infrastructure.CategoriaProductoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.CategoriaProductoJpaRepository;
import co.elchupacabras.tickets.catalogo.infrastructure.InsumoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.InsumoJpaRepository;
import co.elchupacabras.tickets.catalogo.infrastructure.ProductoInsumoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.ProductoInsumoJpaRepository;
import co.elchupacabras.tickets.catalogo.infrastructure.ProductoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.ProductoJpaRepository;
import co.elchupacabras.tickets.catalogo.infrastructure.UnidadMedidaJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.UnidadMedidaJpaRepository;
import co.elchupacabras.tickets.identidad.infrastructure.SedeJpaEntity;
import co.elchupacabras.tickets.identidad.infrastructure.SedeJpaRepository;
import co.elchupacabras.tickets.identidad.infrastructure.UsuarioJpaEntity;
import co.elchupacabras.tickets.identidad.infrastructure.UsuarioJpaRepository;
import co.elchupacabras.tickets.pedidos.infrastructure.PedidoDetalleJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.PedidoJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.PedidoJpaRepository;
import co.elchupacabras.tickets.pedidos.infrastructure.SolicitudDetalleJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.SolicitudJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.SolicitudJpaRepository;
import co.elchupacabras.tickets.shared.domain.CanalSolicitud;
import co.elchupacabras.tickets.shared.domain.EstadoPedido;
import co.elchupacabras.tickets.shared.domain.EstadoReserva;
import co.elchupacabras.tickets.shared.domain.ModalidadPedido;
import co.elchupacabras.tickets.shared.domain.TipoInsumo;
import co.elchupacabras.tickets.shared.domain.TipoProducto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 3. Pruebas de Persistencia y Consultas (@DataJpaTest con H2)
 * Comprueba que las consultas @Query en JPQL con JOIN FETCH y bloqueo pesimista
 * funcionen contra la base de datos H2 en memoria siguiendo el patrón AAA.
 */
@DataJpaTest
@ActiveProfiles("test")
class InventarioYPedidoRepositoryDataJpaTest {

    @Autowired
    private SedeJpaRepository sedeRepository;
    @Autowired
    private UsuarioJpaRepository usuarioRepository;
    @Autowired
    private JornadaJpaRepository jornadaRepository;
    @Autowired
    private UnidadMedidaJpaRepository unidadRepository;
    @Autowired
    private CategoriaProductoJpaRepository categoriaRepository;
    @Autowired
    private ProductoJpaRepository productoRepository;
    @Autowired
    private InsumoJpaRepository insumoRepository;
    @Autowired
    private ProductoInsumoJpaRepository recetaRepository;
    @Autowired
    private InventarioJornadaJpaRepository inventarioRepository;
    @Autowired
    private ReservaInventarioJpaRepository reservaRepository;
    @Autowired
    private SolicitudJpaRepository solicitudRepository;
    @Autowired
    private PedidoJpaRepository pedidoRepository;

    private JornadaJpaEntity jornadaActiva;
    private UsuarioJpaEntity cajero;
    private ProductoJpaEntity burritoMixto;
    private InsumoJpaEntity tortillaHarina;
    private InsumoJpaEntity carneRes;

    @BeforeEach
    void setUp() {
        SedeJpaEntity sede = sedeRepository.save(new SedeJpaEntity("Sede Los Patios", "Los Patios", "Norte de Santander"));
        cajero = usuarioRepository.save(new UsuarioJpaEntity("caja_test", "Cajero Test", "$2a$10$hash"));
        jornadaActiva = jornadaRepository.save(new JornadaJpaEntity(sede, LocalDate.of(2026, 11, 26), cajero));

        UnidadMedidaJpaEntity unidadUnd = unidadRepository.save(new UnidadMedidaJpaEntity("UND", "Unidad", "CONTEO", BigDecimal.ONE));
        UnidadMedidaJpaEntity unidadGr = unidadRepository.save(new UnidadMedidaJpaEntity("GR", "Gramos", "MASA", BigDecimal.ONE));
        CategoriaProductoJpaEntity catBurritos = categoriaRepository.save(new CategoriaProductoJpaEntity("Burritos", "Burritos mexicanos"));

        burritoMixto = productoRepository.save(new ProductoJpaEntity(
                catBurritos,
                "BUR-MIX-01",
                "Burrito Mixto",
                TipoProducto.INDIVIDUAL,
                new BigDecimal("18000.00")
        ));

        tortillaHarina = insumoRepository.save(new InsumoJpaEntity(
                unidadUnd,
                "INS-TORT-01",
                "Tortilla de Harina XL",
                TipoInsumo.INGREDIENTE,
                new BigDecimal("10.000")
        ));

        carneRes = insumoRepository.save(new InsumoJpaEntity(
                unidadGr,
                "INS-RES-01",
                "Carne de Res Desmechada",
                TipoInsumo.INGREDIENTE,
                new BigDecimal("200.000")
        ));
    }

    @Test
    @DisplayName("1. Consulta @Query con bloqueo pesimista (findAndLockByJornadaIdAndInsumoIds): Retorna insumos ordenados por insumo.id ASC y actualiza reservas cruzadas")
    void findAndLockByJornadaIdAndInsumoIds_retornaInsumosOrdenadosYActualizaDisponibilidad() {
        // Arrange: Guardamos 2 insumos en inventario_jornada y simulamos reserva activa y reserva liberada
        InventarioJornadaJpaEntity invTortilla = inventarioRepository.save(
                new InventarioJornadaJpaEntity(jornadaActiva, tortillaHarina, new BigDecimal("20.000"))
        );
        InventarioJornadaJpaEntity invCarne = inventarioRepository.save(
                new InventarioJornadaJpaEntity(jornadaActiva, carneRes, new BigDecimal("1000.000"))
        );

        // Pedido 1 toma 2 tortillas y 300g de carne (ACTIVA)
        invTortilla.reservar(new BigDecimal("2.000"));
        invCarne.reservar(new BigDecimal("300.000"));
        inventarioRepository.saveAll(List.of(invTortilla, invCarne));

        ReservaInventarioJpaEntity resActiva = reservaRepository.save(
                new ReservaInventarioJpaEntity(101L, invCarne, new BigDecimal("300.000"))
        );

        // Pedido 2 había reservado 200g de carne pero fue cancelado antes de preparación (LIBERADA)
        invCarne.reservar(new BigDecimal("200.000"));
        ReservaInventarioJpaEntity resLiberada = reservaRepository.save(
                new ReservaInventarioJpaEntity(102L, invCarne, new BigDecimal("200.000"))
        );
        resLiberada.marcarLiberada(OffsetDateTime.now());
        invCarne.liberarReserva(new BigDecimal("200.000"));
        reservaRepository.save(resLiberada);
        inventarioRepository.save(invCarne);

        // Act: Ejecutamos las consultas @Query personalizadas contra H2
        List<InventarioJornadaJpaEntity> bloqueados = inventarioRepository.findAndLockByJornadaIdAndInsumoIds(
                jornadaActiva.getId(),
                Set.of(carneRes.getId(), tortillaHarina.getId())
        );
        List<ReservaInventarioJpaEntity> reservasPedido101 = reservaRepository.findByPedidoIdWithInventario(101L);
        List<ReservaInventarioJpaEntity> reservasPedido102 = reservaRepository.findByPedidoIdWithInventario(102L);

        // Assert: Verifica ordenamiento estricto por insumo.id ASC (prevención de deadlocks ADR-002)
        // y que la reserva liberada del pedido 102 no afecte el stock disponible (queda 700g disponibles y 300g reservados)
        assertThat(bloqueados).hasSize(2);
        assertThat(bloqueados.get(0).getInsumo().getId()).isLessThan(bloqueados.get(1).getInsumo().getId());

        InventarioJornadaJpaEntity carneActual = bloqueados.stream()
                .filter(ij -> ij.getInsumo().getId().equals(carneRes.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(carneActual.getCantidadDisponible()).isEqualByComparingTo("700.000");
        assertThat(carneActual.getCantidadReservada()).isEqualByComparingTo("300.000");
        assertThat(reservasPedido101).singleElement().extracting(ReservaInventarioJpaEntity::getEstado)
                .isEqualTo(EstadoReserva.ACTIVA);
        assertThat(reservasPedido102).singleElement().extracting(ReservaInventarioJpaEntity::getEstado)
                .isEqualTo(EstadoReserva.LIBERADA);
    }

    @Test
    @DisplayName("2. Consultas @Query de Recetas y Cola de Cocina: Filtra pedidos EN_PREPARACION e ignora pedidos cancelados o ya despachados")
    void findRecetasYComandasEnPreparacion_filtraPorEstadoYOrdenaPorLlegada() {
        // Arrange: Guardamos la receta del Burrito Mixto (1 tortilla + 200g carne)
        recetaRepository.save(new ProductoInsumoJpaEntity(burritoMixto, tortillaHarina, new BigDecimal("1.000")));
        recetaRepository.save(new ProductoInsumoJpaEntity(burritoMixto, carneRes, new BigDecimal("200.000")));

        // Creamos 2 pedidos: uno EN_PREPARACION y otro CANCELADO_REEMBOLSO_PENDIENTE (que debe ser ignorado por Cocina)
        SolicitudJpaEntity sol1 = new SolicitudJpaEntity(
                jornadaActiva, cajero.getId(), CanalSolicitud.PRESENCIAL, ModalidadPedido.CONSUMO_LOCAL, "Cliente Activo", null, null
        );
        sol1.agregarDetalle(new SolicitudDetalleJpaEntity(sol1, burritoMixto, 1, "Con guacamole"));
        sol1.marcarConvertida();
        sol1 = solicitudRepository.save(sol1);

        PedidoJpaEntity pedidoEnCocina = new PedidoJpaEntity(sol1, "PED-20261126-0001", cajero.getId());
        pedidoEnCocina.agregarDetalle(new PedidoDetalleJpaEntity(
                pedidoEnCocina, burritoMixto.getId(), burritoMixto.getCodigo(), burritoMixto.getNombre(), 1, burritoMixto.getPrecio(), "Con guacamole"
        ));
        pedidoEnCocina.transicionarEstado(EstadoPedido.EN_PREPARACION, "CAJA");
        pedidoRepository.save(pedidoEnCocina);

        SolicitudJpaEntity sol2 = new SolicitudJpaEntity(
                jornadaActiva, cajero.getId(), CanalSolicitud.WHATSAPP, ModalidadPedido.RECOGER, "Cliente Cancelado", null, null
        );
        sol2.agregarDetalle(new SolicitudDetalleJpaEntity(sol2, burritoMixto, 2, null));
        sol2.marcarConvertida();
        sol2 = solicitudRepository.save(sol2);

        PedidoJpaEntity pedidoCancelado = new PedidoJpaEntity(sol2, "PED-20261126-0002", cajero.getId());
        pedidoCancelado.agregarDetalle(new PedidoDetalleJpaEntity(
                pedidoCancelado, burritoMixto.getId(), burritoMixto.getCodigo(), burritoMixto.getNombre(), 2, burritoMixto.getPrecio(), null
        ));
        pedidoCancelado.transicionarEstado(EstadoPedido.CANCELADO_REEMBOLSO_PENDIENTE, "PROPIETARIO");
        pedidoRepository.save(pedidoCancelado);

        // Act
        List<ProductoInsumoJpaEntity> receta = recetaRepository.findRecetasByProductoIds(List.of(burritoMixto.getId()));
        List<PedidoJpaEntity> colaCocina = pedidoRepository.findByJornadaIdAndEstadoWithDetallesOrderByCreadoEnAsc(
                jornadaActiva.getId(),
                EstadoPedido.EN_PREPARACION
        );

        // Assert
        assertThat(receta).hasSize(2);
        assertThat(colaCocina).hasSize(1);
        assertThat(colaCocina.get(0).getCodigoPublico()).isEqualTo("PED-20261126-0001");
        assertThat(colaCocina.get(0).getDetalles()).hasSize(1);
    }
}
