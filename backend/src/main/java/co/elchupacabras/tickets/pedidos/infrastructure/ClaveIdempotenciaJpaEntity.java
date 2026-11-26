package co.elchupacabras.tickets.pedidos.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "clave_idempotencia",
        uniqueConstraints = {
                @UniqueConstraint(name = "clave_idempotencia_usuario_id_operacion_clave_key", columnNames = {"usuario_id", "operacion", "clave"})
        },
        indexes = {
                @Index(name = "ix_idempotencia_expiracion", columnList = "expira_en")
        }
)
public class ClaveIdempotenciaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "operacion", nullable = false, length = 60)
    private String operacion;

    @Column(name = "clave", nullable = false, length = 120)
    private String clave;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "hash_solicitud", nullable = false, length = 128)
    private String hashSolicitud;

    @Column(name = "codigo_respuesta")
    private Short codigoRespuesta;

    @Column(name = "respuesta", columnDefinition = "TEXT")
    private String respuesta;

    @Column(name = "creada_en", nullable = false)
    private OffsetDateTime creadaEn = OffsetDateTime.now();

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    protected ClaveIdempotenciaJpaEntity() {
    }

    public ClaveIdempotenciaJpaEntity(
            Long usuarioId,
            String operacion,
            String clave,
            Long pedidoId,
            String hashSolicitud,
            Short codigoRespuesta,
            String respuesta,
            OffsetDateTime expiraEn
    ) {
        this.usuarioId = usuarioId;
        this.operacion = operacion;
        this.clave = clave;
        this.pedidoId = pedidoId;
        this.hashSolicitud = hashSolicitud;
        this.codigoRespuesta = codigoRespuesta;
        this.respuesta = respuesta;
        this.creadaEn = OffsetDateTime.now();
        this.expiraEn = expiraEn;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getOperacion() {
        return operacion;
    }

    public String getClave() {
        return clave;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public String getHashSolicitud() {
        return hashSolicitud;
    }

    public Short getCodigoRespuesta() {
        return codigoRespuesta;
    }

    public String getRespuesta() {
        return respuesta;
    }
}
