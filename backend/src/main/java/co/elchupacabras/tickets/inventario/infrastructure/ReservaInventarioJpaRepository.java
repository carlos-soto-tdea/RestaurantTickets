package co.elchupacabras.tickets.inventario.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservaInventarioJpaRepository extends JpaRepository<ReservaInventarioJpaEntity, Long> {

    @Query("""
            SELECT r FROM ReservaInventarioJpaEntity r
            JOIN FETCH r.inventarioJornada ij
            WHERE r.pedidoId = :pedidoId
            """)
    List<ReservaInventarioJpaEntity> findByPedidoIdWithInventario(@Param("pedidoId") Long pedidoId);
}
