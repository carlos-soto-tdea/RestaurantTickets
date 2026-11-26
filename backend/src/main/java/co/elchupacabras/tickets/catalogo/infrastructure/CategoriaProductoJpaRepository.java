package co.elchupacabras.tickets.catalogo.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaProductoJpaRepository extends JpaRepository<CategoriaProductoJpaEntity, Long> {
}
