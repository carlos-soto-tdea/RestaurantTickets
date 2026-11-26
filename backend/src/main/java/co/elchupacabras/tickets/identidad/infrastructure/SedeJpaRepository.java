package co.elchupacabras.tickets.identidad.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SedeJpaRepository extends JpaRepository<SedeJpaEntity, Long> {
    Optional<SedeJpaEntity> findByNombre(String nombre);
}
