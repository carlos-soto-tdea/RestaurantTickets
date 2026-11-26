package co.elchupacabras.tickets.pedidos.web;

import co.elchupacabras.tickets.shared.domain.CanalSolicitud;
import co.elchupacabras.tickets.shared.domain.ModalidadPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SolicitudValidarRequest(
        @NotNull(message = "La jornada es obligatoria")
        Long jornadaId,
        @NotNull(message = "El canal es obligatorio (PRESENCIAL o WHATSAPP)")
        CanalSolicitud canal,
        @NotNull(message = "La modalidad es obligatoria")
        ModalidadPedido modalidad,
        @NotBlank(message = "El nombre del cliente es obligatorio (RN-009)")
        String nombreCliente,
        Long zonaDomicilioId,
        String direccionTexto,
        @NotEmpty(message = "La solicitud debe tener al menos un ítem")
        @Valid
        List<ItemSolicitudRequest> items
) {
    public record ItemSolicitudRequest(
            @NotNull(message = "El identificador del producto es obligatorio")
            Long productoId,
            @Min(value = 1, message = "La cantidad debe ser mayor que cero")
            int cantidad,
            String preferencias
    ) {
    }
}
