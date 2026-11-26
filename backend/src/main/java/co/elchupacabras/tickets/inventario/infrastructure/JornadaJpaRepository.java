package co.elchupacabras.tickets.inventario.infrastructure;

import co.elchupacabras.tickets.shared.domain.EstadoJornada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JornadaJpaRepository extends JpaRepository<JornadaJpaEntity, Long> {
    Optional<JornadaJpaEntity> findFirstByEstadoOrderByAbiertaEnDesc(EstadoJornada estado);
}
