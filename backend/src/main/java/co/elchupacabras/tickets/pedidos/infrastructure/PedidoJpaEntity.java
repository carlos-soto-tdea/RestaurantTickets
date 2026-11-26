package co.elchupacabras.tickets.pedidos.infrastructure;

import co.elchupacabras.tickets.inventario.infrastructure.JornadaJpaEntity;
import co.elchupacabras.tickets.shared.domain.AccesoDenegadoDominioException;
import co.elchupacabras.tickets.shared.domain.CanalSolicitud;
import co.elchupacabras.tickets.shared.domain.ConcurrenciaInventarioException;
import co.elchupacabras.tickets.shared.domain.EstadoPedido;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "pedido",
        indexes = {
                @Index(name = "ix_pedido_jornada_estado", columnList = "jornada_id, estado, creado_en"),
                @Index(name = "ix_pedido_fecha_modalidad", columnList = "creado_en, modalidad"),
                @Index(name = "ix_pedido_nombre_fecha", columnList = "nombre_cliente, creado_en")
        }
)
public class PedidoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    private SolicitudJpaEntity solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jornada_id", nullable = false)
    private JornadaJpaEntity jornada;

    @Column(name = "codigo_publico", nullable = false, unique = true, length = 40)
    private String codigoPublico;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalSolicitud canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false, length = 30)
    private ModalidadPedido modalidad;

    @Column(name = "nombre_cliente", nullable = false, length = 120)
    private String nombreCliente;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "costo_domicilio", nullable = false, precision = 14, scale = 2)
    private BigDecimal costoDomicilio = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoPedido estado = EstadoPedido.PAGADO_REGISTRADO;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PedidoDetalleJpaEntity> detalles = new ArrayList<>();

    protected PedidoJpaEntity() {
    }

    public PedidoJpaEntity(SolicitudJpaEntity solicitud, String codigoPublico, Long creadoPor) {
        this.solicitud = solicitud;
        this.jornada = solicitud.getJornada();
        this.codigoPublico = codigoPublico;
        this.canal = solicitud.getCanal();
        this.modalidad = solicitud.getModalidad();
        this.nombreCliente = solicitud.getNombreCliente();
        this.subtotal = solicitud.getSubtotal();
        this.costoDomicilio = solicitud.getCostoDomicilio();
        this.total = solicitud.getTotal();
        this.estado = EstadoPedido.PAGADO_REGISTRADO;
        this.creadoPor = creadoPor;
        this.creadoEn = OffsetDateTime.now();
        this.actualizadoEn = this.creadoEn;
    }

    public void agregarDetalle(PedidoDetalleJpaEntity detalle) {
        this.detalles.add(detalle);
    }

    /**
     * Máquina de estados del Pedido (RN-021, RN-025, RN-026, RN-027, RN-034).
     * Cocina tiene estrictamente prohibido cambiar estados de un pedido (403 Forbidden).
     */
    public void transicionarEstado(EstadoPedido nuevoEstado, String rolActor) {
        if ("COCINA".equalsIgnoreCase(rolActor)) {
            throw new AccesoDenegadoDominioException(
                    "COCINA_SOLO_LECTURA",
                    "El rol COCINA no está autorizado para modificar el estado de un pedido (RN-021, RN-027)"
            );
        }

        boolean transicionValida = switch (this.estado) {
            case PAGADO_REGISTRADO -> nuevoEstado == EstadoPedido.EN_PREPARACION
                    || (nuevoEstado == EstadoPedido.CANCELADO_REEMBOLSO_PENDIENTE && esPropietario(rolActor));
            case EN_PREPARACION -> (nuevoEstado == EstadoPedido.LISTO_PARA_DESPACHO && esDespachoOPropietario(rolActor))
                    || (nuevoEstado == EstadoPedido.REEMBOLSO_PENDIENTE && esPropietario(rolActor));
            case LISTO_PARA_DESPACHO -> nuevoEstado == EstadoPedido.LISTO_PARA_RECOGER
                    || (nuevoEstado == EstadoPedido.EN_RUTA_DOMICILIO && this.modalidad == ModalidadPedido.DOMICILIO)
                    || (nuevoEstado == EstadoPedido.ENTREGADO && this.modalidad == ModalidadPedido.CONSUMO_LOCAL);
            case LISTO_PARA_RECOGER -> nuevoEstado == EstadoPedido.ENTREGADO;
            case EN_RUTA_DOMICILIO -> nuevoEstado == EstadoPedido.ENTREGADO || nuevoEstado == EstadoPedido.NO_ENTREGADO;
            case NO_ENTREGADO -> nuevoEstado == EstadoPedido.EN_RUTA_DOMICILIO
                    || nuevoEstado == EstadoPedido.LISTO_PARA_RECOGER
                    || (nuevoEstado == EstadoPedido.REEMBOLSO_PENDIENTE && esPropietario(rolActor));
            case CANCELADO_REEMBOLSO_PENDIENTE, REEMBOLSO_PENDIENTE -> nuevoEstado == EstadoPedido.REEMBOLSADO;
            case ENTREGADO -> nuevoEstado == EstadoPedido.ENTREGADO_CON_AJUSTE;
            case REEMBOLSADO, ENTREGADO_CON_AJUSTE -> false;
        };

        if (!transicionValida) {
            throw new ConcurrenciaInventarioException(
                    "TRANSICION_ESTADO_INVALIDA",
                    "No es posible pasar el pedido de " + this.estado + " a " + nuevoEstado + " con el rol " + rolActor
            );
        }

        this.estado = nuevoEstado;
        this.actualizadoEn = OffsetDateTime.now();
    }

    private boolean esPropietario(String rol) {
        return "PROPIETARIO".equalsIgnoreCase(rol);
    }

    private boolean esDespachoOPropietario(String rol) {
        return "DESPACHO".equalsIgnoreCase(rol) || "PROPIETARIO".equalsIgnoreCase(rol);
    }

    public Long getId() {
        return id;
    }

    public SolicitudJpaEntity getSolicitud() {
        return solicitud;
    }

    public JornadaJpaEntity getJornada() {
        return jornada;
    }

    public String getCodigoPublico() {
        return codigoPublico;
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

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getCostoDomicilio() {
        return costoDomicilio;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public Long getCreadoPor() {
        return creadoPor;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public Long getVersion() {
        return version;
    }

    public List<PedidoDetalleJpaEntity> getDetalles() {
        return detalles;
    }
}
