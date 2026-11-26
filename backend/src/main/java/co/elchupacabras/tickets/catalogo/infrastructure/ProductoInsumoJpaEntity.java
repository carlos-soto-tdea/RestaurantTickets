package co.elchupacabras.tickets.catalogo.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(
        name = "producto_insumo",
        indexes = {
                @Index(name = "ix_producto_insumo_insumo", columnList = "insumo_id")
        }
)
public class ProductoInsumoJpaEntity {

    @EmbeddedId
    private ProductoInsumoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("productoId")
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoJpaEntity producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("insumoId")
    @JoinColumn(name = "insumo_id", nullable = false)
    private InsumoJpaEntity insumo;

    @Column(name = "cantidad_requerida", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadRequerida;

    protected ProductoInsumoJpaEntity() {
    }

    public ProductoInsumoJpaEntity(ProductoJpaEntity producto, InsumoJpaEntity insumo, BigDecimal cantidadRequerida) {
        this.id = new ProductoInsumoId(producto.getId(), insumo.getId());
        this.producto = producto;
        this.insumo = insumo;
        this.cantidadRequerida = cantidadRequerida;
    }

    public ProductoInsumoId getId() {
        return id;
    }

    public ProductoJpaEntity getProducto() {
        return producto;
    }

    public InsumoJpaEntity getInsumo() {
        return insumo;
    }

    public BigDecimal getCantidadRequerida() {
        return cantidadRequerida;
    }
}
