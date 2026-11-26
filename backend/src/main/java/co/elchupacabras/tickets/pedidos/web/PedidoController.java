package co.elchupacabras.tickets.pedidos.web;

import co.elchupacabras.tickets.pedidos.application.ConfirmadorPedidoService;
import co.elchupacabras.tickets.shared.domain.EstadoPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final ConfirmadorPedidoService confirmadorPedidoService;

    public PedidoController(ConfirmadorPedidoService confirmadorPedidoService) {
        this.confirmadorPedidoService = confirmadorPedidoService;
    }

    public record CambioEstadoRequest(
            @NotNull(message = "El nuevo estado es obligatorio")
            EstadoPedido nuevoEstado,
            String observacion
    ) {
    }

    @PostMapping("/confirmar")
    public ResponseEntity<PedidoConfirmadoResponse> confirmarPedido(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Usuario-Id", defaultValue = "1") Long usuarioId,
            @RequestHeader(value = "X-Rol-Usuario", defaultValue = "CAJA") String rolUsuario,
            @Valid @RequestBody PedidoConfirmarRequest request
    ) {
        ConfirmadorPedidoService.ResultadoConfirmacion resultado =
                confirmadorPedidoService.confirmarPedido(idempotencyKey, usuarioId, rolUsuario, request);

        HttpStatus status = resultado.reintentoIdempotente() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(resultado.response());
    }

    @PostMapping("/{id}/enviar-cocina")
    public ResponseEntity<PedidoConfirmadoResponse> enviarACocina(
            @PathVariable("id") Long pedidoId,
            @RequestHeader(value = "X-Usuario-Id", defaultValue = "1") Long usuarioId,
            @RequestHeader(value = "X-Rol-Usuario", defaultValue = "CAJA") String rolUsuario
    ) {
        PedidoConfirmadoResponse response = confirmadorPedidoService.cambiarEstadoPedido(
                pedidoId,
                EstadoPedido.EN_PREPARACION,
                usuarioId,
                rolUsuario,
                "Pedido puesto a disposición de Cocina"
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PedidoConfirmadoResponse> cambiarEstado(
            @PathVariable("id") Long pedidoId,
            @RequestHeader(value = "X-Usuario-Id", defaultValue = "1") Long usuarioId,
            @RequestHeader(value = "X-Rol-Usuario", defaultValue = "DESPACHO") String rolUsuario,
            @Valid @RequestBody CambioEstadoRequest request
    ) {
        PedidoConfirmadoResponse response = confirmadorPedidoService.cambiarEstadoPedido(
                pedidoId,
                request.nuevoEstado(),
                usuarioId,
                rolUsuario,
                request.observacion()
        );
        return ResponseEntity.ok(response);
    }
}
