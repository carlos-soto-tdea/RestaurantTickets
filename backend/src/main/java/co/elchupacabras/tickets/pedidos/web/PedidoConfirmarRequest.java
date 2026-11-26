package co.elchupacabras.tickets.pedidos.web;

import co.elchupacabras.tickets.shared.domain.MedioPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PedidoConfirmarRequest(
        @NotNull(message = "Los datos de la solicitud son obligatorios")
        @Valid
        SolicitudValidarRequest solicitud,
        @NotNull(message = "Los datos del pago son obligatorios")
        @Valid
        PagoConfirmarRequest pago
) {
    public record PagoConfirmarRequest(
            @NotNull(message = "El medio de pago es obligatorio (EFECTIVO o TRANSFERENCIA)")
            MedioPago medio,
            BigDecimal montoRecibido,
            Boolean transferenciaVerificada,
            String referenciaExterna
    ) {
    }
}
