package co.elchupacabras.tickets.domicilios.infrastructure;

import co.elchupacabras.tickets.shared.domain.EstadoDomicilio;
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
        name = "domicilio",
        indexes = {
                @Index(name = "ix_domicilio_estado_domiciliario", columnList = "estado, domiciliario_id")
        }
)
public class DomicilioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false, unique = true)
    private Long pedidoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zona_domicilio_id", nullable = false)
    private ZonaDomicilioJpaEntity zonaDomicilio;

    @Column(name = "domiciliario_id")
    private Long domiciliarioId;

    @Column(name = "direccion_texto", nullable = false, length = 300)
    private String direccionTexto;

    @Column(name = "zona_nombre_snapshot", nullable = false, length = 100)
    private String zonaNombreSnapshot;

    @Column(name = "tarifa_aplicada", nullable = false, precision = 14, scale = 2)
    private BigDecimal tarifaAplicada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoDomicilio estado = EstadoDomicilio.PENDIENTE;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    protected DomicilioJpaEntity() {
    }

    public DomicilioJpaEntity(Long pedidoId, ZonaDomicilioJpaEntity zonaDomicilio, String direccionTexto) {
        this.pedidoId = pedidoId;
        this.zonaDomicilio = zonaDomicilio;
        this.direccionTexto = direccionTexto;
        this.zonaNombreSnapshot = zonaDomicilio.getNombre();
        this.tarifaAplicada = zonaDomicilio.getTarifa();
        this.estado = EstadoDomicilio.PENDIENTE;
    }

    public void asignarYDespachar(Long domiciliarioId) {
        this.domiciliarioId = domiciliarioId;
        this.estado = EstadoDomicilio.EN_RUTA;
        this.actualizadoEn = OffsetDateTime.now();
    }

    public void marcarResultado(EstadoDomicilio nuevoEstado) {
        this.estado = nuevoEstado;
        this.actualizadoEn = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public ZonaDomicilioJpaEntity getZonaDomicilio() {
        return zonaDomicilio;
    }

    public Long getDomiciliarioId() {
        return domiciliarioId;
    }

    public String getDireccionTexto() {
        return direccionTexto;
    }

    public String getZonaNombreSnapshot() {
        return zonaNombreSnapshot;
    }

    public BigDecimal getTarifaAplicada() {
        return tarifaAplicada;
    }

    public EstadoDomicilio getEstado() {
        return estado;
    }
}
