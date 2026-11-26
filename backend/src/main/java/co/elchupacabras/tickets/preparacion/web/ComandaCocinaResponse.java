package co.elchupacabras.tickets.preparacion.web;

import co.elchupacabras.tickets.shared.domain.ModalidadPedido;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO de Proyección de Minimización de Datos para Cocina (RNF-008, RN-012, RN-021):
 * Expone exclusivamente identificador público, modalidad, hora y líneas con preferencias.
 * CERO campos de precios, totales, pagos, referencias Nequi o direcciones de domicilio.
 */
public record ComandaCocinaResponse(
        String codigoPublico,
        String nombreCliente,
        ModalidadPedido modalidad,
        OffsetDateTime creadoEn,
        List<LineaComandaCocina> items
) {
    public record LineaComandaCocina(
            String productoNombre,
            int cantidad,
            String preferencias
    ) {
    }
}
