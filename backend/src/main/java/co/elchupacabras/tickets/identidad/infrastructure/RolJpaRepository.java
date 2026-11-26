package co.elchupacabras.tickets.identidad.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolJpaRepository extends JpaRepository<RolJpaEntity, Long> {
    Optional<RolJpaEntity> findByCodigo(String codigo);
}
