package co.elchupacabras.tickets.inventario.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface InventarioJornadaJpaRepository extends JpaRepository<InventarioJornadaJpaEntity, Long> {

    @Query("""
            SELECT ij FROM InventarioJornadaJpaEntity ij
            JOIN FETCH ij.insumo i
            WHERE ij.jornada.id = :jornadaId
              AND ij.insumo.id IN :insumoIds
            ORDER BY ij.insumo.id ASC
            """)
    List<InventarioJornadaJpaEntity> findByJornadaIdAndInsumoIds(
            @Param("jornadaId") Long jornadaId,
            @Param("insumoIds") Collection<Long> insumoIds
    );

    /**
     * Bloqueo pesimista ordenado por insumo_id ASC (ADR-002 / RNF-004) para prevenir deadlocks
     * y sobreventa bajo concurrencia durante la confirmación atómica del pedido.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ij FROM InventarioJornadaJpaEntity ij
            JOIN FETCH ij.insumo i
            WHERE ij.jornada.id = :jornadaId
              AND ij.insumo.id IN :insumoIds
            ORDER BY ij.insumo.id ASC
            """)
    List<InventarioJornadaJpaEntity> findAndLockByJornadaIdAndInsumoIds(
            @Param("jornadaId") Long jornadaId,
            @Param("insumoIds") Collection<Long> insumoIds
    );
}
