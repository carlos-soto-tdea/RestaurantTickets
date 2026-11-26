package co.elchupacabras.tickets.pedidos.infrastructure;

import co.elchupacabras.tickets.domicilios.infrastructure.ZonaDomicilioJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.JornadaJpaEntity;
import co.elchupacabras.tickets.shared.domain.CanalSolicitud;
import co.elchupacabras.tickets.shared.domain.EstadoSolicitud;
import co.elchupacabras.tickets.shared.domain.ModalidadPedido;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "solicitud",
        indexes = {
                @Index(name = "ix_solicitud_jornada_estado", columnList = "jornada_id, estado, creada_en"),
                @Index(name = "ix_solicitud_nombre_fecha", columnList = "nombre_cliente, creada_en")
        }
)
public class SolicitudJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jornada_id", nullable = false)
    private JornadaJpaEntity jornada;

    @Column(name = "creada_por", nullable = false)
    private Long creadaPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_domicilio_id")
    private ZonaDomicilioJpaEntity zonaDomicilio;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalSolicitud canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false, length = 30)
    private ModalidadPedido modalidad;

    @Column(name = "nombre_cliente", nullable = false, length = 120)
    private String nombreCliente;

    @Column(name = "direccion_texto", length = 300)
    private String direccionTexto;

    @Column(name = "costo_domicilio", nullable = false, precision = 14, scale = 2)
    private BigDecimal costoDomicilio = BigDecimal.ZERO;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE_PAGO;

    @Column(name = "creada_en", nullable = false)
    private OffsetDateTime creadaEn = OffsetDateTime.now();

    @Column(name = "actualizada_en", nullable = false)
    private OffsetDateTime actualizadaEn = OffsetDateTime.now();

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SolicitudDetalleJpaEntity> detalles = new ArrayList<>();

    protected SolicitudJpaEntity() {
    }

    public SolicitudJpaEntity(
            JornadaJpaEntity jornada,
            Long creadaPor,
            CanalSolicitud canal,
            ModalidadPedido modalidad,
            String nombreCliente,
            ZonaDomicilioJpaEntity zonaDomicilio,
            String direccionTexto
    ) {
        this.jornada = jornada;
        this.creadaPor = creadaPor;
        this.canal = canal;
        this.modalidad = modalidad;
        this.nombreCliente = nombreCliente;
        this.zonaDomicilio = zonaDomicilio;
        this.direccionTexto = direccionTexto;
        this.costoDomicilio = zonaDomicilio != null ? zonaDomicilio.getTarifa() : BigDecimal.ZERO;
        this.estado = EstadoSolicitud.PENDIENTE_PAGO;
    }

    public void agregarDetalle(SolicitudDetalleJpaEntity detalle) {
        this.detalles.add(detalle);
        recalcularTotales();
    }

    public void recalcularTotales() {
        this.subtotal = detalles.stream()
                .map(SolicitudDetalleJpaEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = this.subtotal.add(this.costoDomicilio);
        this.actualizadaEn = OffsetDateTime.now();
    }

    public void marcarConvertida() {
        this.estado = EstadoSolicitud.CONVERTIDA;
        this.actualizadaEn = OffsetDateTime.now();
    }

    public void cancelarSinPago() {
        this.estado = EstadoSolicitud.CANCELADA_SIN_PAGO;
        this.actualizadaEn = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public JornadaJpaEntity getJornada() {
        return jornada;
    }

    public CanalSolicitud getCanal() {
        return canal;
    }

    public ModalidadPedido getModalidad() {
        return modalidad;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public ZonaDomicilioJpaEntity getZonaDomicilio() {
        return zonaDomicilio;
    }

    public String getDireccionTexto() {
        return direccionTexto;
    }

    public BigDecimal getCostoDomicilio() {
        return costoDomicilio;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public List<SolicitudDetalleJpaEntity> getDetalles() {
        return detalles;
    }
}
