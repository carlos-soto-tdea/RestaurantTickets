package co.elchupacabras.tickets.pedidos.application;

import co.elchupacabras.tickets.pedidos.domain.CambioEstadoPedidoEvent;
import co.elchupacabras.tickets.pedidos.infrastructure.HistorialEstadoPedidoJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.HistorialEstadoPedidoJpaRepository;
import co.elchupacabras.tickets.shared.infrastructure.AuditoriaJpaEntity;
import co.elchupacabras.tickets.shared.infrastructure.AuditoriaJpaRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Patrón Observer / EventListener + Principio de Responsabilidad Única (SRP - SOLID):
 * Desacopla el registro inmutable en historial_estado_pedido y auditoria (RNF-018)
 * de la lógica transaccional principal de confirmación y despacho.
 */
@Component
public class AuditoriaHistorialEventListener {

    private final HistorialEstadoPedidoJpaRepository historialRepository;
    private final AuditoriaJpaRepository auditoriaRepository;

    public AuditoriaHistorialEventListener(
            HistorialEstadoPedidoJpaRepository historialRepository,
            AuditoriaJpaRepository auditoriaRepository
    ) {
        this.historialRepository = historialRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @EventListener
    public void onCambioEstadoPedido(CambioEstadoPedidoEvent event) {
        HistorialEstadoPedidoJpaEntity historial = new HistorialEstadoPedidoJpaEntity(
                event.pedidoId(),
                event.estadoAnterior(),
                event.estadoNuevo(),
                event.usuarioId(),
                event.origen(),
                event.observacion()
        );
        historialRepository.save(historial);

        String anteriorJson = event.estadoAnterior() != null
                ? "{\"estado\":\"" + event.estadoAnterior().name() + "\"}"
                : null;
        String nuevoJson = "{\"estado\":\"" + event.estadoNuevo().name() + "\",\"codigoPublico\":\"" + event.codigoPublico() + "\"}";

        AuditoriaJpaEntity auditoria = new AuditoriaJpaEntity(
                event.usuarioId(),
                event.estadoAnterior() == null ? "CREAR_PEDIDO" : "CAMBIAR_ESTADO_PEDIDO",
                "PEDIDO",
                String.valueOf(event.pedidoId()),
                anteriorJson,
                nuevoJson
        );
        auditoriaRepository.save(auditoria);
    }
}
