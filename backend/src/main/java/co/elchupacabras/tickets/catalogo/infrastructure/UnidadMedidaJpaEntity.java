package co.elchupacabras.tickets.catalogo.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "unidad_medida")
public class UnidadMedidaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "magnitud", nullable = false, length = 30)
    private String magnitud;

    @Column(name = "factor_base", nullable = false, precision = 18, scale = 6)
    private BigDecimal factorBase = BigDecimal.ONE;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    protected UnidadMedidaJpaEntity() {
    }

    public UnidadMedidaJpaEntity(String codigo, String nombre, String magnitud, BigDecimal factorBase) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.magnitud = magnitud;
        this.factorBase = factorBase;
        this.activa = true;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getMagnitud() {
        return magnitud;
    }

    public BigDecimal getFactorBase() {
        return factorBase;
    }

    public boolean isActiva() {
        return activa;
    }
}
