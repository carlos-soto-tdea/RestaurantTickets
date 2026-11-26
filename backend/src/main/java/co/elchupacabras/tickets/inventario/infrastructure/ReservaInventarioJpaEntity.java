package co.elchupacabras.tickets.inventario.infrastructure;

import co.elchupacabras.tickets.shared.domain.EstadoReserva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "reserva_inventario",
        uniqueConstraints = {
                @UniqueConstraint(name = "reserva_inventario_pedido_id_inventario_jornada_id_key", columnNames = {"pedido_id", "inventario_jornada_id"})
        },
        indexes = {
                @Index(name = "ix_reserva_inventario_estado", columnList = "inventario_jornada_id, estado")
        }
)
public class ReservaInventarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventario_jornada_id", nullable = false)
    private InventarioJornadaJpaEntity inventarioJornada;

    @Column(name = "cantidad", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoReserva estado = EstadoReserva.ACTIVA;

    @Column(name = "reservada_en", nullable = false)
    private OffsetDateTime reservadaEn = OffsetDateTime.now();

    @Column(name = "finalizada_en")
    private OffsetDateTime finalizadaEn;

    protected ReservaInventarioJpaEntity() {
    }

    public ReservaInventarioJpaEntity(Long pedidoId, InventarioJornadaJpaEntity inventarioJornada, BigDecimal cantidad) {
        this.pedidoId = pedidoId;
        this.inventarioJornada = inventarioJornada;
        this.cantidad = cantidad;
        this.estado = EstadoReserva.ACTIVA;
        this.reservadaEn = OffsetDateTime.now();
    }

    public void marcarConsumida(OffsetDateTime ahora) {
        this.estado = EstadoReserva.CONSUMIDA;
        this.finalizadaEn = ahora;
    }

    public void marcarLiberada(OffsetDateTime ahora) {
        this.estado = EstadoReserva.LIBERADA;
        this.finalizadaEn = ahora;
    }

    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public InventarioJornadaJpaEntity getInventarioJornada() {
        return inventarioJornada;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public OffsetDateTime getReservadaEn() {
        return reservadaEn;
    }

    public OffsetDateTime getFinalizadaEn() {
        return finalizadaEn;
    }
}
