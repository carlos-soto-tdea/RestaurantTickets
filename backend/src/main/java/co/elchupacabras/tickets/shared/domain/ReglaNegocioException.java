package co.elchupacabras.tickets.shared.domain;

import java.util.List;

/**
 * Excepción de dominio que representa el incumplimiento de una Regla de Negocio (RN-001 a RN-045)
 * y se traduce a HTTP 422 Unprocessable Entity según el contrato T3-01.
 */
public class ReglaNegocioException extends RuntimeException {

    private final String codigo;
    private final List<String> detalles;

    public ReglaNegocioException(String codigo, String mensaje) {
        this(codigo, mensaje, List.of());
    }

    public ReglaNegocioException(String codigo, String mensaje, List<String> detalles) {
        super(mensaje);
        this.codigo = codigo;
        this.detalles = detalles != null ? List.copyOf(detalles) : List.of();
    }

    public String getCodigo() {
        return codigo;
    }

    public List<String> getDetalles() {
        return detalles;
    }
}
