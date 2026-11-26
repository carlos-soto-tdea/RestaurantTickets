package co.elchupacabras.tickets.pedidos.domain;

import co.elchupacabras.tickets.shared.domain.MedioPago;

import java.math.BigDecimal;

public record DatosPagoComando(
        MedioPago medio,
        BigDecimal montoRecibido,
        Boolean transferenciaVerificada,
        String referenciaExterna
) {
}
