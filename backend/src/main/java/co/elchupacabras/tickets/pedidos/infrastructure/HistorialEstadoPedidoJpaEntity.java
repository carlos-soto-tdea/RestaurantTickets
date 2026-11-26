package co.elchupacabras.tickets.pedidos.infrastructure;

import co.elchupacabras.tickets.shared.domain.EstadoPedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "historial_estado_pedido",
        indexes = {
                @Index(name = "ix_historial_pedido_fecha", columnList = "pedido_id, creado_en")
        }
)
public class HistorialEstadoPedidoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 40)
    private EstadoPedido estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 40)
    private EstadoPedido estadoNuevo;

    @Column(name = "cambiado_por")
    private Long cambiadoPor;

    @Column(name = "origen", nullable = false, length = 30)
    private String origen;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    protected HistorialEstadoPedidoJpaEntity() {
    }

    public HistorialEstadoPedidoJpaEntity(
            Long pedidoId,
            EstadoPedido estadoAnterior,
            EstadoPedido estadoNuevo,
            Long cambiadoPor,
            String origen,
            String observacion
    ) {
        this.pedidoId = pedidoId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.cambiadoPor = cambiadoPor;
        this.origen = origen;
        this.observacion = observacion;
        this.creadoEn = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public EstadoPedido getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoPedido getEstadoNuevo() {
        return estadoNuevo;
    }

    public Long getCambiadoPor() {
        return cambiadoPor;
    }

    public String getOrigen() {
        return origen;
    }

    public String getObservacion() {
        return observacion;
    }
}
