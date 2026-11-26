package co.elchupacabras.tickets.domicilios.infrastructure;

import co.elchupacabras.tickets.shared.domain.ResultadoEntrega;
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
        name = "intento_entrega",
        uniqueConstraints = {
                @UniqueConstraint(name = "intento_entrega_domicilio_id_numero_intento_key", columnNames = {"domicilio_id", "numero_intento"})
        },
        indexes = {
                @Index(name = "ix_intento_entrega_fecha", columnList = "domicilio_id, resultado_en")
        }
)
public class IntentoEntregaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domicilio_id", nullable = false)
    private DomicilioJpaEntity domicilio;

    @Column(name = "numero_intento", nullable = false)
    private short numeroIntento;

    @Column(name = "domiciliario_id", nullable = false)
    private Long domiciliarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", length = 20)
    private ResultadoEntrega resultado;

    @Column(name = "motivo_no_entrega", length = 300)
    private String motivoNoEntrega;

    @Column(name = "costo_adicional", nullable = false, precision = 14, scale = 2)
    private BigDecimal costoAdicional = BigDecimal.ZERO;

    @Column(name = "salida_en", nullable = false)
    private OffsetDateTime salidaEn = OffsetDateTime.now();

    @Column(name = "resultado_en")
    private OffsetDateTime resultadoEn;

    protected IntentoEntregaJpaEntity() {
    }

    public IntentoEntregaJpaEntity(DomicilioJpaEntity domicilio, short numeroIntento, Long domiciliarioId) {
        this.domicilio = domicilio;
        this.numeroIntento = numeroIntento;
        this.domiciliarioId = domiciliarioId;
        this.salidaEn = OffsetDateTime.now();
    }

    public void registrarResultado(ResultadoEntrega resultado, String motivoNoEntrega) {
        this.resultado = resultado;
        this.motivoNoEntrega = motivoNoEntrega;
        this.resultadoEn = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public DomicilioJpaEntity getDomicilio() {
        return domicilio;
    }

    public short getNumeroIntento() {
        return numeroIntento;
    }

    public ResultadoEntrega getResultado() {
        return resultado;
    }

    public String getMotivoNoEntrega() {
        return motivoNoEntrega;
    }
}
