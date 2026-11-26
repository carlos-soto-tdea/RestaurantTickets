package co.elchupacabras.tickets.pedidos.infrastructure;

import co.elchupacabras.tickets.shared.domain.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoJpaRepository extends JpaRepository<PedidoJpaEntity, Long> {

    Optional<PedidoJpaEntity> findByCodigoPublico(String codigoPublico);

    @Query("""
            SELECT DISTINCT p FROM PedidoJpaEntity p
            LEFT JOIN FETCH p.detalles
            WHERE p.jornada.id = :jornadaId
              AND p.estado = :estado
            ORDER BY p.creadoEn ASC
            """)
    List<PedidoJpaEntity> findByJornadaIdAndEstadoWithDetallesOrderByCreadoEnAsc(
            @Param("jornadaId") Long jornadaId,
            @Param("estado") EstadoPedido estado
    );
}
