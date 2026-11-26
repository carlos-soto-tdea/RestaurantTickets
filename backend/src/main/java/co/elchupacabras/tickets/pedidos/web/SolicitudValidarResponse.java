package co.elchupacabras.tickets.pedidos.web;

import java.math.BigDecimal;
import java.util.List;

public record SolicitudValidarResponse(
        boolean solicitudValida,
        BigDecimal subtotal,
        BigDecimal costoDomicilio,
        BigDecimal total,
        List<String> faltantes
) {
}
