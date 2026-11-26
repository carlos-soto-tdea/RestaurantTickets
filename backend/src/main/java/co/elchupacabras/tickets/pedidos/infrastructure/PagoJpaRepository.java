package co.elchupacabras.tickets.pedidos.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoJpaRepository extends JpaRepository<PagoJpaEntity, Long> {
    List<PagoJpaEntity> findByPedidoId(Long pedidoId);
}
