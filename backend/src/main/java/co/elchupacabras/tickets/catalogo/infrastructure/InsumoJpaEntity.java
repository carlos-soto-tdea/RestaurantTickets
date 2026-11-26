package co.elchupacabras.tickets.catalogo.infrastructure;

import co.elchupacabras.tickets.shared.domain.TipoInsumo;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "insumo")
public class InsumoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_medida_id", nullable = false)
    private UnidadMedidaJpaEntity unidadMedida;

    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoInsumo tipo;

    @Column(name = "umbral_bajo", nullable = false, precision = 14, scale = 3)
    private BigDecimal umbralBajo = BigDecimal.ZERO;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    protected InsumoJpaEntity() {
    }

    public InsumoJpaEntity(
            UnidadMedidaJpaEntity unidadMedida,
            String codigo,
            String nombre,
            TipoInsumo tipo,
            BigDecimal umbralBajo
    ) {
        this.unidadMedida = unidadMedida;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.umbralBajo = umbralBajo;
        this.activo = true;
    }

    public Long getId() {
        return id;
    }

    public UnidadMedidaJpaEntity getUnidadMedida() {
        return unidadMedida;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoInsumo getTipo() {
        return tipo;
    }

    public BigDecimal getUmbralBajo() {
        return umbralBajo;
    }

    public boolean isActivo() {
        return activo;
    }
}
