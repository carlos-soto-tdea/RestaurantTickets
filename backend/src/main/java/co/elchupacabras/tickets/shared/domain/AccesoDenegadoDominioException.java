package co.elchupacabras.tickets.shared.domain;

/**
 * Excepción de dominio lanzada cuando un rol autenticado intenta ejecutar una operación
 * para la cual no está autorizado (p. ej. COCINA intentando cambiar el estado de un pedido - RN-021,
 * o un no-PROPIETARIO intentando aprobar una devolución - RN-038, RNF-005).
 * Se traduce a HTTP 403 Forbidden y genera registro en auditoria.
 */
public class AccesoDenegadoDominioException extends RuntimeException {

    private final String codigo;

    public AccesoDenegadoDominioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
