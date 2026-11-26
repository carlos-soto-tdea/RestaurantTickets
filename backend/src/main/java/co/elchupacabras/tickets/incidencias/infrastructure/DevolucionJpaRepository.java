package co.elchupacabras.tickets.incidencias.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevolucionJpaRepository extends JpaRepository<DevolucionJpaEntity, Long> {
    List<DevolucionJpaEntity> findByPedidoIdOrderBySolicitadaEnDesc(Long pedidoId);
}
