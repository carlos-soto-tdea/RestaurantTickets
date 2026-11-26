package co.elchupacabras.tickets.cierre.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CierreJornadaJpaRepository extends JpaRepository<CierreJornadaJpaEntity, Long> {
    Optional<CierreJornadaJpaEntity> findByJornadaId(Long jornadaId);
}
