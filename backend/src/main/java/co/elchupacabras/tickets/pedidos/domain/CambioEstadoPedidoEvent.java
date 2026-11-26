package co.elchupacabras.tickets.pedidos.domain;

import co.elchupacabras.tickets.shared.domain.EstadoPedido;

/**
 * Evento de dominio publicado al confirmar un pedido o cambiar su estado operativo (RNF-018).
 */
public record CambioEstadoPedidoEvent(
        Long pedidoId,
        String codigoPublico,
        EstadoPedido estadoAnterior,
        EstadoPedido estadoNuevo,
        Long usuarioId,
        String origen,
        String observacion
) {
}
