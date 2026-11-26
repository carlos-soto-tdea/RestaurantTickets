package co.elchupacabras.tickets.pedidos;

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
import co.elchupacabras.tickets.inventario.infrastructure.InventarioJornadaJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.InventarioJornadaJpaRepository;
import co.elchupacabras.tickets.inventario.infrastructure.JornadaJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.JornadaJpaRepository;
import co.elchupacabras.tickets.inventario.infrastructure.ReservaInventarioJpaRepository;
import co.elchupacabras.tickets.pedidos.infrastructure.HistorialEstadoPedidoJpaRepository;
import co.elchupacabras.tickets.shared.domain.EstadoReserva;
import co.elchupacabras.tickets.shared.domain.TipoInsumo;
import co.elchupacabras.tickets.shared.domain.TipoProducto;
import co.elchupacabras.tickets.shared.infrastructure.AuditoriaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoPedidoCriticoTest {

    @Autowired
    private MockMvc mockMvc;
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
    private HistorialEstadoPedidoJpaRepository historialRepository;
    @Autowired
    private AuditoriaJpaRepository auditoriaRepository;

    private JornadaJpaEntity jornada;
    private ProductoJpaEntity tacoPastor;
    private InsumoJpaEntity carnePastor;
    private InventarioJornadaJpaEntity inventarioCarne;

    @BeforeEach
    void setUp() {
        SedeJpaEntity sede = sedeRepository.save(new SedeJpaEntity("Sede Principal Los Patios", "Los Patios", "Norte de Santander"));
        UsuarioJpaEntity cajero = usuarioRepository.save(new UsuarioJpaEntity("caja01", "Carlos Cajero", "$2a$10$hash"));
        jornada = jornadaRepository.save(new JornadaJpaEntity(sede, LocalDate.of(2026, 11, 25), cajero));

        UnidadMedidaJpaEntity gramos = unidadRepository.save(new UnidadMedidaJpaEntity("GR", "Gramos", "MASA", BigDecimal.ONE));
        CategoriaProductoJpaEntity catTacos = categoriaRepository.save(new CategoriaProductoJpaEntity("Tacos", "Tacos mexicanos"));

        tacoPastor = productoRepository.save(new ProductoJpaEntity(
                catTacos,
                "TAC-PASTOR-01",
                "Taco al Pastor",
                TipoProducto.INDIVIDUAL,
                new BigDecimal("12000.00")
        ));

        carnePastor = insumoRepository.save(new InsumoJpaEntity(
                gramos,
                "INS-CARNE-PASTOR",
                "Carne al Pastor",
                TipoInsumo.INGREDIENTE,
                new BigDecimal("100.000")
        ));

        // Receta: 1 taco requiere 150 gramos de carne
        recetaRepository.save(new ProductoInsumoJpaEntity(tacoPastor, carnePastor, new BigDecimal("150.000")));

        // Inventario inicial: 300 gramos disponibles (alcanza exactamente para 2 tacos)
        inventarioCarne = inventarioRepository.save(new InventarioJornadaJpaEntity(jornada, carnePastor, new BigDecimal("300.000")));
    }

    @Test
    @DisplayName("HU-001 / T3-01: Validar solicitud sin reservar stock, confirmar en efectivo (201), reintento idempotente (200), proyección Cocina sin precios (RNF-008) y consumo en Despacho")
    void flujoCompletoExitosoEIdempotente() throws Exception {
        String validarPayload = """
                {
                  "jornadaId": %d,
                  "canal": "PRESENCIAL",
                  "modalidad": "PARA_LLEVAR",
                  "nombreCliente": "Carlos",
                  "items": [
                    { "productoId": %d, "cantidad": 2, "preferencias": "Sin cebolla" }
                  ]
                }
                """.formatted(jornada.getId(), tacoPastor.getId());

        // 1. Validación previa (200 OK) - RN-004: NO reserva inventario
        mockMvc.perform(post("/api/v1/solicitudes/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validarPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitudValida").value(true))
                .andExpect(jsonPath("$.total").value(24000.00));

        assertThat(inventarioRepository.findById(inventarioCarne.getId()).orElseThrow().getCantidadReservada())
                .isEqualByComparingTo("0.000");

        // 2. Confirmación con Idempotency-Key (201 Created) - RN-016: Reserva atómica
        String idempotencyKey = UUID.randomUUID().toString();
        String confirmarPayload = """
                {
                  "solicitud": %s,
                  "pago": {
                    "medio": "EFECTIVO",
                    "montoRecibido": 30000.00
                  }
                }
                """.formatted(validarPayload);

        mockMvc.perform(post("/api/v1/pedidos/confirmar")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Rol-Usuario", "CAJA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmarPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreCliente").value("Carlos"))
                .andExpect(jsonPath("$.estado").value("PAGADO_REGISTRADO"))
                .andExpect(jsonPath("$.total").value(24000.00))
                .andExpect(jsonPath("$.vuelto").value(6000.00));

        InventarioJornadaJpaEntity invPostConfirmacion = inventarioRepository.findById(inventarioCarne.getId()).orElseThrow();
        assertThat(invPostConfirmacion.getCantidadDisponible()).isEqualByComparingTo("0.000");
        assertThat(invPostConfirmacion.getCantidadReservada()).isEqualByComparingTo("300.000");

        // 3. Reintento con el mismo Idempotency-Key (RNF-010) -> 200 OK sin duplicar pedido ni reserva
        mockMvc.perform(post("/api/v1/pedidos/confirmar")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Rol-Usuario", "CAJA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmarPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO_REGISTRADO"));

        assertThat(reservaRepository.findAll()).hasSize(1);
        Long pedidoId = reservaRepository.findAll().get(0).getPedidoId();

        // 4. Caja envía pedido a Cocina -> EN_PREPARACION
        mockMvc.perform(post("/api/v1/pedidos/{id}/enviar-cocina", pedidoId)
                        .header("X-Rol-Usuario", "CAJA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"));

        // 5. Cocina consulta comandas (RNF-008): CERO precios, totales o datos de pago expuestos
        mockMvc.perform(get("/api/v1/cocina/comandas")
                        .param("jornadaId", String.valueOf(jornada.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreCliente").value("Carlos"))
                .andExpect(jsonPath("$[0].items[0].preferencias").value("Sin cebolla"))
                .andExpect(jsonPath("$[0].total").doesNotExist())
                .andExpect(jsonPath("$[0].subtotal").doesNotExist())
                .andExpect(jsonPath("$[0].pago").doesNotExist());

        // 6. Cocina intenta cambiar el estado -> 403 Forbidden y auditado (RN-021, RN-027, RNF-005)
        mockMvc.perform(patch("/api/v1/pedidos/{id}/estado", pedidoId)
                        .header("X-Rol-Usuario", "COCINA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"LISTO_PARA_DESPACHO\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("COCINA_SOLO_LECTURA"));

        // 7. Despacho recibe físicamente de Cocina -> LISTO_PARA_DESPACHO y reserva pasa a CONSUMIDA (RN-016)
        mockMvc.perform(patch("/api/v1/pedidos/{id}/estado", pedidoId)
                        .header("X-Rol-Usuario", "DESPACHO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"LISTO_PARA_DESPACHO\",\"observacion\":\"Recibido en mostrador de despacho\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LISTO_PARA_DESPACHO"));

        assertThat(reservaRepository.findAll().get(0).getEstado()).isEqualTo(EstadoReserva.CONSUMIDA);
        assertThat(historialRepository.findByPedidoIdOrderByCreadoEnAsc(pedidoId)).hasSize(3);
        assertThat(auditoriaRepository.findAll()).isNotEmpty();
    }

    @Test
    @DisplayName("HU-001 / RN-015: Rechaza solicitud con 422 Unprocessable Entity cuando el stock disponible no alcanza")
    void rechazaPorStockInsuficienteCon422() throws Exception {
        String payloadExcesivo = """
                {
                  "jornadaId": %d,
                  "canal": "PRESENCIAL",
                  "modalidad": "CONSUMO_LOCAL",
                  "nombreCliente": "Laura",
                  "items": [
                    { "productoId": %d, "cantidad": 4, "preferencias": "Con todo" }
                  ]
                }
                """.formatted(jornada.getId(), tacoPastor.getId());

        mockMvc.perform(post("/api/v1/solicitudes/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadExcesivo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));
    }

    @Test
    @DisplayName("HU-001 / RN-008: Rechaza confirmación con 422 cuando transferencia Nequi no fue verificada por Caja")
    void rechazaTransferenciaNequiNoVerificadaCon422() throws Exception {
        String confirmarNequiSinVerificar = """
                {
                  "solicitud": {
                    "jornadaId": %d,
                    "canal": "WHATSAPP",
                    "modalidad": "RECOGER",
                    "nombreCliente": "Andres",
                    "items": [
                      { "productoId": %d, "cantidad": 1, "preferencias": null }
                    ]
                  },
                  "pago": {
                    "medio": "TRANSFERENCIA",
                    "transferenciaVerificada": false,
                    "referenciaExterna": "BREB-1122"
                  }
                }
                """.formatted(jornada.getId(), tacoPastor.getId());

        mockMvc.perform(post("/api/v1/pedidos/confirmar")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("X-Rol-Usuario", "CAJA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmarNequiSinVerificar))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("TRANSFERENCIA_NO_VERIFICADA"));
    }
}
