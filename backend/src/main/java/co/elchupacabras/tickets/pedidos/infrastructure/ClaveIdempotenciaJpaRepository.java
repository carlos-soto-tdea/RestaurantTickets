package co.elchupacabras.tickets.pedidos.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClaveIdempotenciaJpaRepository extends JpaRepository<ClaveIdempotenciaJpaEntity, Long> {
    Optional<ClaveIdempotenciaJpaEntity> findByUsuarioIdAndOperacionAndClave(Long usuarioId, String operacion, String clave);
}
