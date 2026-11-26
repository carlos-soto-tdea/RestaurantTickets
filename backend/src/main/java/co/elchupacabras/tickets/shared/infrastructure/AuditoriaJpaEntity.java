package co.elchupacabras.tickets.shared.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "auditoria",
        indexes = {
                @Index(name = "ix_auditoria_entidad", columnList = "entidad, entidad_id, creado_en"),
                @Index(name = "ix_auditoria_usuario_fecha", columnList = "usuario_id, creado_en")
        }
)
public class AuditoriaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    @Column(name = "entidad", nullable = false, length = 80)
    private String entidad;

    @Column(name = "entidad_id", nullable = false, length = 80)
    private String entidadId;

    @Column(name = "datos_anteriores", columnDefinition = "TEXT")
    private String datosAnteriores;

    @Column(name = "datos_nuevos", columnDefinition = "TEXT")
    private String datosNuevos;

    @Column(name = "direccion_ip", length = 60)
    private String direccionIp;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    protected AuditoriaJpaEntity() {
    }

    public AuditoriaJpaEntity(Long usuarioId, String accion, String entidad, String entidadId, String datosAnteriores, String datosNuevos) {
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.datosAnteriores = datosAnteriores;
        this.datosNuevos = datosNuevos;
        this.creadoEn = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getAccion() {
        return accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public String getDatosAnteriores() {
        return datosAnteriores;
    }

    public String getDatosNuevos() {
        return datosNuevos;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
