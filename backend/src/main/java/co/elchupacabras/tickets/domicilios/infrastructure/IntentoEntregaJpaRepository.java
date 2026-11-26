package co.elchupacabras.tickets.domicilios.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntentoEntregaJpaRepository extends JpaRepository<IntentoEntregaJpaEntity, Long> {
    List<IntentoEntregaJpaEntity> findByDomicilioIdOrderByNumeroIntentoAsc(Long domicilioId);
}
