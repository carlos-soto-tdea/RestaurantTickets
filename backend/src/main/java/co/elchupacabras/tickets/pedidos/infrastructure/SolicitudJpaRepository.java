package co.elchupacabras.tickets.pedidos.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudJpaRepository extends JpaRepository<SolicitudJpaEntity, Long> {
}
