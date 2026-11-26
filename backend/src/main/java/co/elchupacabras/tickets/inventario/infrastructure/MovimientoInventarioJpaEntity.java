package co.elchupacabras.tickets.inventario.infrastructure;

import co.elchupacabras.tickets.shared.domain.TipoMovimientoInventario;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "movimiento_inventario",
        indexes = {
                @Index(name = "ix_movimiento_inventario_fecha", columnList = "inventario_jornada_id, creado_en")
        }
)
public class MovimientoInventarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventario_jornada_id", nullable = false)
    private InventarioJornadaJpaEntity inventarioJornada;

    @Column(name = "reserva_id")
    private Long reservaId;

    @Column(name = "realizado_por", nullable = false)
    private Long realizadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoMovimientoInventario tipo;

    @Column(name = "cantidad", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "motivo", nullable = false, length = 300)
    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    protected MovimientoInventarioJpaEntity() {
    }

    public MovimientoInventarioJpaEntity(
            InventarioJornadaJpaEntity inventarioJornada,
            Long reservaId,
            Long realizadoPor,
            TipoMovimientoInventario tipo,
            BigDecimal cantidad,
            String motivo
    ) {
        this.inventarioJornada = inventarioJornada;
        this.reservaId = reservaId;
        this.realizadoPor = realizadoPor;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.creadoEn = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public InventarioJornadaJpaEntity getInventarioJornada() {
        return inventarioJornada;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public Long getRealizadoPor() {
        return realizadoPor;
    }

    public TipoMovimientoInventario getTipo() {
        return tipo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public String getMotivo() {
        return motivo;
    }
}
