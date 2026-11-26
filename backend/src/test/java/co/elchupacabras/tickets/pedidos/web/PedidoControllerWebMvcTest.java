package co.elchupacabras.tickets.pedidos.web;

import co.elchupacabras.tickets.pedidos.application.ConfirmadorPedidoService;
import co.elchupacabras.tickets.shared.domain.AccesoDenegadoDominioException;
import co.elchupacabras.tickets.shared.domain.ConcurrenciaInventarioException;
import co.elchupacabras.tickets.shared.domain.EstadoPedido;
import co.elchupacabras.tickets.shared.domain.ReglaNegocioException;
import co.elchupacabras.tickets.shared.infrastructure.SecurityConfig;
import co.elchupacabras.tickets.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 2. Pruebas de Integración de API (@WebMvcTest + MockMvc)
 * Valida rutas HTTP, validaciones de entrada (@Valid: @NotNull, @NotBlank, @Min)
 * y el mapeo exacto de excepciones (400, 403, 409, 422) en GlobalExceptionHandler.
 */
@WebMvcTest(controllers = PedidoController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class PedidoControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfirmadorPedidoService confirmadorPedidoService;

    @Test
    @DisplayName("1. Validación de campos (400 Bad Request): Rechaza JSON con campos obligatorios nulos o cantidad < 1 (@NotNull, @NotBlank, @Min) antes de llegar al servicio")
    void confirmarPedido_camposInvalidosOCantidadCero_retorna400SinLlamarAlServicio() throws Exception {
        // Arrange: nombreCliente vacío (@NotBlank) y cantidad = 0 (@Min(1))
        String invalidJson = """
                {
                  "solicitud": {
                    "jornadaId": 1,
                    "canal": "PRESENCIAL",
                    "modalidad": "CONSUMO_LOCAL",
                    "nombreCliente": "",
                    "items": [
                      { "productoId": 10, "cantidad": 0, "preferencias": null }
                    ]
                  },
                  "pago": {
                    "medio": "EFECTIVO",
                    "montoRecibido": 20000.00
                  }
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/pedidos/confirmar")
                        .header("Idempotency-Key", "idem-400-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_ENTRADA_INVALIDOS"))
                .andExpect(jsonPath("$.estadoHttp").value(400))
                .andExpect(jsonPath("$.detalles").isArray())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(confirmadorPedidoService, never()).confirmarPedido(any(), any(), any(), any());
    }

    @Test
    @DisplayName("2. Mapeo de excepción HTTP 409 Conflict: GlobalExceptionHandler devuelve esquema estándar ApiErrorResponse cuando ocurre choque de concurrencia")
    void confirmarPedido_conflictoConcurrenciaEnServicio_retorna409ConApiErrorResponse() throws Exception {
        // Arrange
        String validJson = """
                {
                  "solicitud": {
                    "jornadaId": 1,
                    "canal": "PRESENCIAL",
                    "modalidad": "PARA_LLEVAR",
                    "nombreCliente": "Carlos Soto",
                    "items": [
                      { "productoId": 10, "cantidad": 2, "preferencias": "Sin picante" }
                    ]
                  },
                  "pago": {
                    "medio": "EFECTIVO",
                    "montoRecibido": 30000.00
                  }
                }
                """;

        when(confirmadorPedidoService.confirmarPedido(eq("idem-409-test"), any(), any(), any()))
                .thenThrow(new ConcurrenciaInventarioException(
                        "CONFLICTO_INVENTARIO_CONCURRENTE",
                        "Otro pedido agotó el insumo Carne al Pastor simultáneamente (RNF-004)"
                ));

        // Act & Assert
        mockMvc.perform(post("/api/v1/pedidos/confirmar")
                        .header("Idempotency-Key", "idem-409-test")
                        .header("X-Rol-Usuario", "CAJA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_INVENTARIO_CONCURRENTE"))
                .andExpect(jsonPath("$.estadoHttp").value(409))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("3. Mapeo de excepción HTTP 422 Unprocessable Entity: GlobalExceptionHandler devuelve 422 cuando falla regla de negocio (RN-008 / RN-015)")
    void confirmarPedido_reglaNegocioVioladaEnServicio_retorna422ConDetalles() throws Exception {
        // Arrange
        String validJson = """
                {
                  "solicitud": {
                    "jornadaId": 1,
                    "canal": "WHATSAPP",
                    "modalidad": "RECOGER",
                    "nombreCliente": "Kevin Angarita",
                    "items": [
                      { "productoId": 10, "cantidad": 1, "preferencias": null }
                    ]
                  },
                  "pago": {
                    "medio": "TRANSFERENCIA",
                    "transferenciaVerificada": false,
                    "referenciaExterna": ""
                  }
                }
                """;

        when(confirmadorPedidoService.confirmarPedido(eq("idem-422-test"), any(), any(), any()))
                .thenThrow(new ReglaNegocioException(
                        "TRANSFERENCIA_NO_VERIFICADA",
                        "La transferencia Nequi debe ser verificada manualmente por Caja (RN-008)",
                        List.of("transferenciaVerificada=false")
                ));

        // Act & Assert
        mockMvc.perform(post("/api/v1/pedidos/confirmar")
                        .header("Idempotency-Key", "idem-422-test")
                        .header("X-Rol-Usuario", "CAJA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("TRANSFERENCIA_NO_VERIFICADA"))
                .andExpect(jsonPath("$.estadoHttp").value(422))
                .andExpect(jsonPath("$.detalles[0]").value("transferenciaVerificada=false"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("4. Mapeo de excepción HTTP 403 Forbidden y Camino Feliz 201 Created: Verifica respuesta estándar en autorización y creación")
    void cambiarEstadoYConfirmar_mapea403Y201Correctamente() throws Exception {
        // Arrange 403 Forbidden
        when(confirmadorPedidoService.cambiarEstadoPedido(eq(55L), eq(EstadoPedido.LISTO_PARA_DESPACHO), any(), eq("COCINA"), any()))
                .thenThrow(new AccesoDenegadoDominioException(
                        "COCINA_SOLO_LECTURA",
                        "Cocina tiene prohibido modificar estados de pedidos (RN-021)"
                ));

        // Act & Assert 403
        mockMvc.perform(patch("/api/v1/pedidos/55/estado")
                        .header("X-Rol-Usuario", "COCINA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"LISTO_PARA_DESPACHO\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("COCINA_SOLO_LECTURA"))
                .andExpect(jsonPath("$.estadoHttp").value(403));

        // Arrange 201 Created
        PedidoConfirmadoResponse responseEsperada = new PedidoConfirmadoResponse(
                55L,
                "PED-20261126-0001",
                "Juan Jaimes",
                EstadoPedido.PAGADO_REGISTRADO,
                new BigDecimal("12000.00"),
                new BigDecimal("0.00"),
                new BigDecimal("12000.00"),
                new BigDecimal("8000.00"),
                OffsetDateTime.now()
        );
        when(confirmadorPedidoService.confirmarPedido(eq("idem-201-ok"), any(), eq("CAJA"), any()))
                .thenReturn(new ConfirmadorPedidoService.ResultadoConfirmacion(false, responseEsperada));

        String validJson = """
                {
                  "solicitud": {
                    "jornadaId": 1,
                    "canal": "PRESENCIAL",
                    "modalidad": "CONSUMO_LOCAL",
                    "nombreCliente": "Juan Jaimes",
                    "items": [
                      { "productoId": 10, "cantidad": 1, "preferencias": null }
                    ]
                  },
                  "pago": {
                    "medio": "EFECTIVO",
                    "montoRecibido": 20000.00
                  }
                }
                """;

        // Act & Assert 201
        mockMvc.perform(post("/api/v1/pedidos/confirmar")
                        .header("Idempotency-Key", "idem-201-ok")
                        .header("X-Rol-Usuario", "CAJA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.pedidoId").value("PED-20261126-0001"))
                .andExpect(jsonPath("$.estado").value("PAGADO_REGISTRADO"))
                .andExpect(jsonPath("$.vuelto").value(8000.00));
    }
}
