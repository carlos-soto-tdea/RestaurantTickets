package co.elchupacabras.tickets.cierre.infrastructure;

import co.elchupacabras.tickets.inventario.infrastructure.JornadaJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cierre_jornada")
public class CierreJornadaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jornada_id", nullable = false, unique = true)
    private JornadaJpaEntity jornada;

    @Column(name = "aprobado_por", nullable = false)
    private Long aprobadoPor;

    @Column(name = "ventas_brutas", nullable = false, precision = 14, scale = 2)
    private BigDecimal ventasBrutas;

    @Column(name = "devoluciones_ejecutadas", nullable = false, precision = 14, scale = 2)
    private BigDecimal devolucionesEjecutadas;

    @Column(name = "ventas_netas", nullable = false, precision = 14, scale = 2)
    private BigDecimal ventasNetas;

    @Column(name = "gastos_totales", nullable = false, precision = 14, scale = 2)
    private BigDecimal gastosTotales;

    @Column(name = "domicilios_cobrados", nullable = false, precision = 14, scale = 2)
    private BigDecimal domiciliosCobrados;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @Column(name = "aprobado_en", nullable = false)
    private OffsetDateTime aprobadoEn = OffsetDateTime.now();

    @OneToMany(mappedBy = "cierreJornada", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiferenciaCajaJpaEntity> diferencias = new ArrayList<>();

    protected CierreJornadaJpaEntity() {
    }

    public CierreJornadaJpaEntity(
            JornadaJpaEntity jornada,
            Long aprobadoPor,
            BigDecimal ventasBrutas,
            BigDecimal devolucionesEjecutadas,
            BigDecimal gastosTotales,
            BigDecimal domiciliosCobrados,
            String observaciones
    ) {
        this.jornada = jornada;
        this.aprobadoPor = aprobadoPor;
        this.ventasBrutas = ventasBrutas;
        this.devolucionesEjecutadas = devolucionesEjecutadas;
        this.ventasNetas = ventasBrutas.subtract(devolucionesEjecutadas);
        this.gastosTotales = gastosTotales;
        this.domiciliosCobrados = domiciliosCobrados;
        this.observaciones = observaciones;
        this.aprobadoEn = OffsetDateTime.now();
    }

    public void agregarDiferencia(DiferenciaCajaJpaEntity diferencia) {
        this.diferencias.add(diferencia);
    }

    public Long getId() {
        return id;
    }

    public JornadaJpaEntity getJornada() {
        return jornada;
    }

    public BigDecimal getVentasBrutas() {
        return ventasBrutas;
    }

    public BigDecimal getDevolucionesEjecutadas() {
        return devolucionesEjecutadas;
    }

    public BigDecimal getVentasNetas() {
        return ventasNetas;
    }

    public List<DiferenciaCajaJpaEntity> getDiferencias() {
        return diferencias;
    }
}
