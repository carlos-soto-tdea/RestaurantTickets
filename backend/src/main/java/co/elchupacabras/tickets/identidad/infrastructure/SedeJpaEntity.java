package co.elchupacabras.tickets.identidad.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sede")
public class SedeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "municipio", nullable = false, length = 100)
    private String municipio = "Los Patios";

    @Column(name = "departamento", nullable = false, length = 100)
    private String departamento = "Norte de Santander";

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    protected SedeJpaEntity() {
    }

    public SedeJpaEntity(String nombre, String municipio, String departamento) {
        this.nombre = nombre;
        this.municipio = municipio;
        this.departamento = departamento;
        this.activa = true;
        this.creadoEn = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getMunicipio() {
        return municipio;
    }

    public String getDepartamento() {
        return departamento;
    }

    public boolean isActiva() {
        return activa;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
