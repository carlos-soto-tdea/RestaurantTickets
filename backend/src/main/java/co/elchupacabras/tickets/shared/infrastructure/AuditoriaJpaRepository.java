package co.elchupacabras.tickets.shared.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaJpaRepository extends JpaRepository<AuditoriaJpaEntity, Long> {
    List<AuditoriaJpaEntity> findByEntidadAndEntidadIdOrderByCreadoEnDesc(String entidad, String entidadId);
}
