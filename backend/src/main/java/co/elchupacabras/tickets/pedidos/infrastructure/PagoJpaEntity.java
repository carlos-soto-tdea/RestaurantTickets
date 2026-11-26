package co.elchupacabras.tickets.pedidos.infrastructure;

import co.elchupacabras.tickets.shared.domain.MedioPago;
import co.elchupacabras.tickets.shared.domain.TipoPago;
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
        name = "pago",
        indexes = {
                @Index(name = "ix_pago_pedido_tipo", columnList = "pedido_id, tipo, confirmado_en"),
                @Index(name = "ix_pago_fecha_medio", columnList = "confirmado_en, medio")
        }
)
public class PagoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoJpaEntity pedido;

    @Column(name = "confirmado_por", nullable = false)
    private Long confirmadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoPago tipo = TipoPago.PEDIDO;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio", nullable = false, length = 20)
    private MedioPago medio;

    @Column(name = "monto_pagado", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "monto_recibido", precision = 14, scale = 2)
    private BigDecimal montoRecibido;

    @Column(name = "vuelto", nullable = false, precision = 14, scale = 2)
    private BigDecimal vuelto = BigDecimal.ZERO;

    @Column(name = "transferencia_verificada")
    private Boolean transferenciaVerificada;

    @Column(name = "referencia_externa", length = 100)
    private String referenciaExterna;

    @Column(name = "confirmado_en", nullable = false)
    private OffsetDateTime confirmadoEn = OffsetDateTime.now();

    protected PagoJpaEntity() {
    }

    public PagoJpaEntity(
            PedidoJpaEntity pedido,
            Long confirmadoPor,
            TipoPago tipo,
            MedioPago medio,
            BigDecimal montoPagado,
            BigDecimal montoRecibido,
            BigDecimal vuelto,
            Boolean transferenciaVerificada,
            String referenciaExterna
    ) {
        this.pedido = pedido;
        this.confirmadoPor = confirmadoPor;
        this.tipo = tipo;
        this.medio = medio;
        this.montoPagado = montoPagado;
        this.montoRecibido = montoRecibido;
        this.vuelto = vuelto;
        this.transferenciaVerificada = transferenciaVerificada;
        this.referenciaExterna = referenciaExterna;
        this.confirmadoEn = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public PedidoJpaEntity getPedido() {
        return pedido;
    }

    public TipoPago getTipo() {
        return tipo;
    }

    public MedioPago getMedio() {
        return medio;
    }

    public BigDecimal getMontoPagado() {
        return montoPagado;
    }

    public BigDecimal getMontoRecibido() {
        return montoRecibido;
    }

    public BigDecimal getVuelto() {
        return vuelto;
    }

    public Boolean getTransferenciaVerificada() {
        return transferenciaVerificada;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }
}
