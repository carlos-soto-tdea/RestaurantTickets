package co.elchupacabras.tickets.shared.domain;

/**
 * Excepción de dominio para conflictos de concurrencia al reservar inventario (RNF-004)
 * o transiciones de estado inválidas (RN-026), traducida a HTTP 409 Conflict.
 */
public class ConcurrenciaInventarioException extends RuntimeException {

    private final String codigo;

    public ConcurrenciaInventarioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
