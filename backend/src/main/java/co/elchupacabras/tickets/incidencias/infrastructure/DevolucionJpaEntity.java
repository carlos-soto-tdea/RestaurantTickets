package co.elchupacabras.tickets.incidencias.infrastructure;

import co.elchupacabras.tickets.shared.domain.AccesoDenegadoDominioException;
import co.elchupacabras.tickets.shared.domain.EstadoDevolucion;
import co.elchupacabras.tickets.shared.domain.MedioPago;
import co.elchupacabras.tickets.shared.domain.TipoDevolucion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "devolucion",
        indexes = {
                @Index(name = "ix_devolucion_pedido_estado", columnList = "pedido_id, estado, solicitada_en")
        }
)
public class DevolucionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "incidencia_id")
    private Long incidenciaId;

    @Column(name = "autorizada_por")
    private Long autorizadaPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoDevolucion tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoDevolucion estado = EstadoDevolucion.SOLICITADA;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "monto", nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago_original", nullable = false, length = 20)
    private MedioPago medioPagoOriginal;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_devolucion", length = 20)
    private MedioPago medioDevolucion;

    @Column(name = "referencia_pago", length = 100)
    private String referenciaPago;

    @Column(name = "referencia_devolucion", length = 100)
    private String referenciaDevolucion;

    @Column(name = "solicitada_en", nullable = false)
    private OffsetDateTime solicitadaEn = OffsetDateTime.now();

    @Column(name = "autorizada_en")
    private OffsetDateTime autorizadaEn;

    @Column(name = "ejecutada_en")
    private OffsetDateTime ejecutadaEn;

    protected DevolucionJpaEntity() {
    }

    public DevolucionJpaEntity(
            Long pedidoId,
            Long incidenciaId,
            TipoDevolucion tipo,
            String motivo,
            BigDecimal monto,
            MedioPago medioPagoOriginal,
            String referenciaPago
    ) {
        this.pedidoId = pedidoId;
        this.incidenciaId = incidenciaId;
        this.tipo = tipo;
        this.motivo = motivo;
        this.monto = monto;
        this.medioPagoOriginal = medioPagoOriginal;
        this.referenciaPago = referenciaPago;
        this.estado = EstadoDevolucion.SOLICITADA;
        this.solicitadaEn = OffsetDateTime.now();
    }

    /**
     * Regla RN-038 y RNF-005: Toda devolución debe ser aprobada exclusivamente por el PROPIETARIO.
     */
    public void aprobar(Long usuarioId, String rolActor) {
        if (!"PROPIETARIO".equalsIgnoreCase(rolActor)) {
            throw new AccesoDenegadoDominioException(
                    "APROBACION_DEVOLUCION_NO_AUTORIZADA",
                    "Solo el rol PROPIETARIO está autorizado para aprobar devoluciones (RN-038)"
            );
        }
        this.autorizadaPor = usuarioId;
        this.estado = EstadoDevolucion.APROBADA;
        this.autorizadaEn = OffsetDateTime.now();
    }

    public void ejecutar(MedioPago medioDevolucion, String referenciaDevolucion) {
        this.medioDevolucion = medioDevolucion;
        this.referenciaDevolucion = referenciaDevolucion;
        this.estado = EstadoDevolucion.EJECUTADA;
        this.ejecutadaEn = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public Long getAutorizadaPor() {
        return autorizadaPor;
    }

    public TipoDevolucion getTipo() {
        return tipo;
    }

    public EstadoDevolucion getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public BigDecimal getMonto() {
        return monto;
    }
}
