package co.elchupacabras.tickets.pedidos.web;

import co.elchupacabras.tickets.shared.domain.EstadoPedido;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PedidoConfirmadoResponse(
        Long id,
        String pedidoId,
        String nombreCliente,
        EstadoPedido estado,
        BigDecimal subtotal,
        BigDecimal costoDomicilio,
        BigDecimal total,
        BigDecimal vuelto,
        OffsetDateTime creadoEn
) {
}
