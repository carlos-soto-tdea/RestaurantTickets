package co.elchupacabras.tickets.catalogo.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoJpaRepository extends JpaRepository<ProductoJpaEntity, Long> {
    Optional<ProductoJpaEntity> findByCodigo(String codigo);
    List<ProductoJpaEntity> findByActivoTrue();
}
