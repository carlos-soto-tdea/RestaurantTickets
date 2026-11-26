package co.elchupacabras.tickets.pedidos.domain;

import co.elchupacabras.tickets.shared.domain.MedioPago;
import co.elchupacabras.tickets.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Estrategia concreta para validación de pagos en EFECTIVO (RN-003, COMP-04 ck_pago_efectivo).
 */
@Component
public class EfectivoPagoStrategy implements ValidadorPagoStrategy {

    @Override
    public MedioPago medioSoportado() {
        return MedioPago.EFECTIVO;
    }

    @Override
    public BigDecimal validarYCalcularVuelto(DatosPagoComando pago, BigDecimal totalPedido) {
        if (pago.montoRecibido() == null || pago.montoRecibido().compareTo(totalPedido) < 0) {
            throw new ReglaNegocioException(
                    "EFECTIVO_INSUFICIENTE",
                    "El monto en efectivo recibido es menor que el total del pedido (" + totalPedido + ")"
            );
        }
        if (pago.transferenciaVerificada() != null) {
            throw new ReglaNegocioException(
                    "ATRIBUTO_TRANSFERENCIA_INVALIDO_EN_EFECTIVO",
                    "Un pago en efectivo no debe declarar verificación de transferencia"
            );
        }
        return pago.montoRecibido().subtract(totalPedido);
    }
}
