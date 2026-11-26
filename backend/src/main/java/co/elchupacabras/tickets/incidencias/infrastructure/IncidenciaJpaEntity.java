package co.elchupacabras.tickets.incidencias.infrastructure;

import co.elchupacabras.tickets.shared.domain.AtribucionIncidencia;
import co.elchupacabras.tickets.shared.domain.TipoIncidencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "incidencia",
        indexes = {
                @Index(name = "ix_incidencia_pedido_fecha", columnList = "pedido_id, creada_en")
        }
)
public class IncidenciaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoIncidencia tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "atribucion", nullable = false, length = 20)
    private AtribucionIncidencia atribucion;

    @Column(name = "descripcion", nullable = false, length = 800)
    private String descripcion;

    @Column(name = "creada_por", nullable = false)
    private Long creadaPor;

    @Column(name = "creada_en", nullable = false)
    private OffsetDateTime creadaEn = OffsetDateTime.now();

    @Column(name = "cerrada_en")
    private OffsetDateTime cerradaEn;

    protected IncidenciaJpaEntity() {
    }

    public IncidenciaJpaEntity(Long pedidoId, TipoIncidencia tipo, AtribucionIncidencia atribucion, String descripcion, Long creadaPor) {
        this.pedidoId = pedidoId;
        this.tipo = tipo;
        this.atribucion = atribucion;
        this.descripcion = descripcion;
        this.creadaPor = creadaPor;
        this.creadaEn = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public TipoIncidencia getTipo() {
        return tipo;
    }

    public AtribucionIncidencia getAtribucion() {
        return atribucion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
