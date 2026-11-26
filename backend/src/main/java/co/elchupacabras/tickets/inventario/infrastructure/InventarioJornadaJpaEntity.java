package co.elchupacabras.tickets.inventario.infrastructure;

import co.elchupacabras.tickets.catalogo.infrastructure.InsumoJpaEntity;
import co.elchupacabras.tickets.shared.domain.ConcurrenciaInventarioException;
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
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "inventario_jornada",
        uniqueConstraints = {
                @UniqueConstraint(name = "inventario_jornada_jornada_id_insumo_id_key", columnNames = {"jornada_id", "insumo_id"})
        }
)
public class InventarioJornadaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jornada_id", nullable = false)
    private JornadaJpaEntity jornada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private InsumoJpaEntity insumo;

    @Column(name = "cantidad_inicial", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadInicial;

    @Column(name = "cantidad_disponible", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadDisponible;

    @Column(name = "cantidad_reservada", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadReservada = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    protected InventarioJornadaJpaEntity() {
    }

    public InventarioJornadaJpaEntity(JornadaJpaEntity jornada, InsumoJpaEntity insumo, BigDecimal cantidadInicial) {
        this.jornada = jornada;
        this.insumo = insumo;
        this.cantidadInicial = cantidadInicial;
        this.cantidadDisponible = cantidadInicial;
        this.cantidadReservada = BigDecimal.ZERO;
        this.actualizadoEn = OffsetDateTime.now();
    }

    public boolean tieneDisponibilidad(BigDecimal cantidadRequerida) {
        return this.cantidadDisponible.compareTo(cantidadRequerida) >= 0;
    }

    /**
     * Reserva atómica de inventario al confirmar el pago (RN-016, RNF-004).
     * Lanza ConcurrenciaInventarioException (409 Conflict) si el stock disponible es menor a lo requerido.
     */
    public void reservar(BigDecimal cantidadRequerida) {
        if (!tieneDisponibilidad(cantidadRequerida)) {
            throw new ConcurrenciaInventarioException(
                    "CONFLICTO_INVENTARIO_CONCURRENTE",
                    "El insumo '" + insumo.getNombre() + "' agotó su disponibilidad durante la confirmación"
            );
        }
        this.cantidadDisponible = this.cantidadDisponible.subtract(cantidadRequerida);
        this.cantidadReservada = this.cantidadReservada.add(cantidadRequerida);
        this.actualizadoEn = OffsetDateTime.now();
    }

    /**
     * Consume definitivamente la reserva cuando Despacho recibe físicamente el pedido terminado (RN-016, RN-027).
     */
    public void consumirReserva(BigDecimal cantidad) {
        this.cantidadReservada = this.cantidadReservada.subtract(cantidad);
        this.actualizadoEn = OffsetDateTime.now();
    }

    /**
     * Libera la cantidad reservada cuando se aprueba cancelación antes de iniciar preparación (RN-017, RN-034).
     */
    public void liberarReserva(BigDecimal cantidad) {
        this.cantidadReservada = this.cantidadReservada.subtract(cantidad);
        this.cantidadDisponible = this.cantidadDisponible.add(cantidad);
        this.actualizadoEn = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public JornadaJpaEntity getJornada() {
        return jornada;
    }

    public InsumoJpaEntity getInsumo() {
        return insumo;
    }

    public BigDecimal getCantidadInicial() {
        return cantidadInicial;
    }

    public BigDecimal getCantidadDisponible() {
        return cantidadDisponible;
    }

    public BigDecimal getCantidadReservada() {
        return cantidadReservada;
    }

    public Long getVersion() {
        return version;
    }
}
