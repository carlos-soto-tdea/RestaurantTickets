package co.elchupacabras.tickets.domicilios.infrastructure;

import co.elchupacabras.tickets.identidad.infrastructure.SedeJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "zona_domicilio",
        uniqueConstraints = {
                @UniqueConstraint(name = "zona_domicilio_sede_id_nombre_key", columnNames = {"sede_id", "nombre"})
        }
)
public class ZonaDomicilioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private SedeJpaEntity sede;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "municipio", nullable = false, length = 100)
    private String municipio = "Los Patios";

    @Column(name = "tarifa", nullable = false, precision = 14, scale = 2)
    private BigDecimal tarifa;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    protected ZonaDomicilioJpaEntity() {
    }

    public ZonaDomicilioJpaEntity(SedeJpaEntity sede, String nombre, String municipio, BigDecimal tarifa, boolean activa) {
        this.sede = sede;
        this.nombre = nombre;
        this.municipio = municipio;
        this.tarifa = tarifa;
        this.activa = activa;
    }

    public boolean perteneceALosPatiosYEstaActiva() {
        return activa && "Los Patios".equalsIgnoreCase(municipio);
    }

    public Long getId() {
        return id;
    }

    public SedeJpaEntity getSede() {
        return sede;
    }

    public String getNombre() {
        return nombre;
    }

    public String getMunicipio() {
        return municipio;
    }

    public BigDecimal getTarifa() {
        return tarifa;
    }

    public boolean isActiva() {
        return activa;
    }
}
