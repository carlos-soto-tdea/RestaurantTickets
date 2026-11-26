package co.elchupacabras.tickets.domicilios.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DomicilioJpaRepository extends JpaRepository<DomicilioJpaEntity, Long> {
    Optional<DomicilioJpaEntity> findByPedidoId(Long pedidoId);
}
