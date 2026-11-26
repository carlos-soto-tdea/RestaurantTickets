package co.elchupacabras.tickets.catalogo.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ProductoInsumoId implements Serializable {

    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "insumo_id")
    private Long insumoId;

    protected ProductoInsumoId() {
    }

    public ProductoInsumoId(Long productoId, Long insumoId) {
        this.productoId = productoId;
        this.insumoId = insumoId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public Long getInsumoId() {
        return insumoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductoInsumoId that)) return false;
        return Objects.equals(productoId, that.productoId) && Objects.equals(insumoId, that.insumoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productoId, insumoId);
    }
}
