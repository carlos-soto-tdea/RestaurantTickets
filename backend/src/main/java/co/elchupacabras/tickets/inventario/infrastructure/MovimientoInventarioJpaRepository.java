package co.elchupacabras.tickets.inventario.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInventarioJpaRepository extends JpaRepository<MovimientoInventarioJpaEntity, Long> {
    List<MovimientoInventarioJpaEntity> findByInventarioJornadaIdOrderByCreadoEnDesc(Long inventarioJornadaId);
}
