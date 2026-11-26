package co.elchupacabras.tickets.pedidos.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoPedidoJpaRepository extends JpaRepository<HistorialEstadoPedidoJpaEntity, Long> {
    List<HistorialEstadoPedidoJpaEntity> findByPedidoIdOrderByCreadoEnAsc(Long pedidoId);
}
