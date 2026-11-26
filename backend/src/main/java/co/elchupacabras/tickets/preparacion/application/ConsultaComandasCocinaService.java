package co.elchupacabras.tickets.preparacion.application;

import co.elchupacabras.tickets.pedidos.infrastructure.PedidoJpaEntity;
import co.elchupacabras.tickets.pedidos.infrastructure.PedidoJpaRepository;
import co.elchupacabras.tickets.preparacion.web.ComandaCocinaResponse;
import co.elchupacabras.tickets.shared.domain.EstadoPedido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de consulta de solo lectura para Cocina (RN-018, RN-021, RNF-008):
 * Proyecta únicamente los pedidos en estado EN_PREPARACION sin exponer datos financieros ni direcciones.
 */
@Service
public class ConsultaComandasCocinaService {

    private final PedidoJpaRepository pedidoRepository;

    public ConsultaComandasCocinaService(PedidoJpaRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<ComandaCocinaResponse> consultarComandasEnPreparacion(Long jornadaId) {
        List<PedidoJpaEntity> pedidos = pedidoRepository
                .findByJornadaIdAndEstadoWithDetallesOrderByCreadoEnAsc(jornadaId, EstadoPedido.EN_PREPARACION);

        return pedidos.stream()
                .map(p -> new ComandaCocinaResponse(
                        p.getCodigoPublico(),
                        p.getNombreCliente(),
                        p.getModalidad(),
                        p.getCreadoEn(),
                        p.getDetalles().stream()
                                .map(d -> new ComandaCocinaResponse.LineaComandaCocina(
                                        d.getProductoNombre(),
                                        d.getCantidad(),
                                        d.getPreferencias()
                                ))
                                .toList()
                ))
                .toList();
    }
}
