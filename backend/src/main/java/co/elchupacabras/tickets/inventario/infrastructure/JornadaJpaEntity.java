package co.elchupacabras.tickets.inventario.infrastructure;

import co.elchupacabras.tickets.identidad.infrastructure.SedeJpaEntity;
import co.elchupacabras.tickets.identidad.infrastructure.UsuarioJpaEntity;
import co.elchupacabras.tickets.shared.domain.EstadoJornada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "jornada",
        uniqueConstraints = {
                @UniqueConstraint(name = "jornada_sede_id_fecha_operacion_key", columnNames = {"sede_id", "fecha_operacion"})
        }
)
public class JornadaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private SedeJpaEntity sede;

    @Column(name = "fecha_operacion", nullable = false)
    private LocalDate fechaOperacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoJornada estado = EstadoJornada.ABIERTA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "abierta_por", nullable = false)
    private UsuarioJpaEntity abiertaPor;

    @Column(name = "abierta_en", nullable = false)
    private OffsetDateTime abiertaEn = OffsetDateTime.now();

    @Column(name = "cerrada_en")
    private OffsetDateTime cerradaEn;

    protected JornadaJpaEntity() {
    }

    public JornadaJpaEntity(SedeJpaEntity sede, LocalDate fechaOperacion, UsuarioJpaEntity abiertaPor) {
        this.sede = sede;
        this.fechaOperacion = fechaOperacion;
        this.abiertaPor = abiertaPor;
        this.estado = EstadoJornada.ABIERTA;
        this.abiertaEn = OffsetDateTime.now();
    }

    public void cerrar(OffsetDateTime fechaCierre) {
        this.estado = EstadoJornada.CERRADA;
        this.cerradaEn = fechaCierre;
    }

    public Long getId() {
        return id;
    }

    public SedeJpaEntity getSede() {
        return sede;
    }

    public LocalDate getFechaOperacion() {
        return fechaOperacion;
    }

    public EstadoJornada getEstado() {
        return estado;
    }

    public UsuarioJpaEntity getAbiertaPor() {
        return abiertaPor;
    }

    public OffsetDateTime getAbiertaEn() {
        return abiertaEn;
    }

    public OffsetDateTime getCerradaEn() {
        return cerradaEn;
    }
}
