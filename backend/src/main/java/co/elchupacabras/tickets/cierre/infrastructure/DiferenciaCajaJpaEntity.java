package co.elchupacabras.tickets.cierre.infrastructure;

import co.elchupacabras.tickets.shared.domain.MedioPago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(
        name = "diferencia_caja",
        uniqueConstraints = {
                @UniqueConstraint(name = "diferencia_caja_cierre_jornada_id_medio_key", columnNames = {"cierre_jornada_id", "medio"})
        }
)
public class DiferenciaCajaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cierre_jornada_id", nullable = false)
    private CierreJornadaJpaEntity cierreJornada;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio", nullable = false, length = 20)
    private MedioPago medio;

    @Column(name = "valor_esperado", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorEsperado;

    @Column(name = "valor_observado", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorObservado;

    @Column(name = "diferencia", nullable = false, precision = 14, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "justificacion", length = 800)
    private String justificacion;

    @Column(name = "autorizada_por")
    private Long autorizadaPor;

    protected DiferenciaCajaJpaEntity() {
    }

    public DiferenciaCajaJpaEntity(
            CierreJornadaJpaEntity cierreJornada,
            MedioPago medio,
            BigDecimal valorEsperado,
            BigDecimal valorObservado,
            String justificacion,
            Long autorizadaPor
    ) {
        this.cierreJornada = cierreJornada;
        this.medio = medio;
        this.valorEsperado = valorEsperado;
        this.valorObservado = valorObservado;
        this.diferencia = valorObservado.subtract(valorEsperado);
        this.justificacion = justificacion;
        this.autorizadaPor = autorizadaPor;
    }

    public Long getId() {
        return id;
    }

    public MedioPago getMedio() {
        return medio;
    }

    public BigDecimal getValorEsperado() {
        return valorEsperado;
    }

    public BigDecimal getValorObservado() {
        return valorObservado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }
}
