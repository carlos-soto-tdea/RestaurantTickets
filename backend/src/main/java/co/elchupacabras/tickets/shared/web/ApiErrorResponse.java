package co.elchupacabras.tickets.shared.web;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO inmutable estandarizado para respuestas de error HTTP (400, 403, 409, 422) según Taller 3 (T3-01).
 */
public record ApiErrorResponse(
        String codigo,
        String mensaje,
        int estadoHttp,
        List<String> detalles,
        OffsetDateTime timestamp
) {
}
