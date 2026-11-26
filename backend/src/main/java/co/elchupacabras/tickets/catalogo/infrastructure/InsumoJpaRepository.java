package co.elchupacabras.tickets.catalogo.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsumoJpaRepository extends JpaRepository<InsumoJpaEntity, Long> {
    Optional<InsumoJpaEntity> findByCodigo(String codigo);
}
