package co.elchupacabras.tickets.catalogo.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductoInsumoJpaRepository extends JpaRepository<ProductoInsumoJpaEntity, ProductoInsumoId> {

    @Query("""
            SELECT pi FROM ProductoInsumoJpaEntity pi
            JOIN FETCH pi.insumo i
            JOIN FETCH i.unidadMedida
            WHERE pi.producto.id IN :productoIds
            """)
    List<ProductoInsumoJpaEntity> findRecetasByProductoIds(@Param("productoIds") Collection<Long> productoIds);
}
