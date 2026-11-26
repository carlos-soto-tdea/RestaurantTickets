package co.elchupacabras.tickets.pedidos.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(
        name = "pedido_detalle",
        indexes = {
                @Index(name = "ix_pedido_detalle_producto", columnList = "producto_id, pedido_id")
        }
)
public class PedidoDetalleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoJpaEntity pedido;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "producto_codigo", nullable = false, length = 40)
    private String productoCodigo;

    @Column(name = "producto_nombre", nullable = false, length = 120)
    private String productoNombre;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "preferencias", length = 500)
    private String preferencias;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    protected PedidoDetalleJpaEntity() {
    }

    /**
     * Conserva la instantánea comercial inmutable del producto al momento de confirmar el pago (COMP-02, RNF-015).
     */
    public PedidoDetalleJpaEntity(
            PedidoJpaEntity pedido,
            Long productoId,
            String productoCodigo,
            String productoNombre,
            int cantidad,
            BigDecimal precioUnitario,
            String preferencias
    ) {
        this.pedido = pedido;
        this.productoId = productoId;
        this.productoCodigo = productoCodigo;
        this.productoNombre = productoNombre;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.preferencias = preferencias;
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public String getProductoCodigo() {
        return productoCodigo;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public String getPreferencias() {
        return preferencias;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
