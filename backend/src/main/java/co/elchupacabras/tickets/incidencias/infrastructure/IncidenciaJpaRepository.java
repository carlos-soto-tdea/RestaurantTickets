package co.elchupacabras.tickets.incidencias.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenciaJpaRepository extends JpaRepository<IncidenciaJpaEntity, Long> {
    List<IncidenciaJpaEntity> findByPedidoIdOrderByCreadaEnDesc(Long pedidoId);
}
