package co.elchupacabras.tickets.pedidos.domain;

import co.elchupacabras.tickets.shared.domain.MedioPago;
import co.elchupacabras.tickets.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Estrategia concreta para validación de pagos por TRANSFERENCIA en Nequi vía Bre-B (RN-003, RN-008).
 * Exige verificación manual explícita por parte de Caja y referencia externa no vacía.
 */
@Component
public class TransferenciaNequiPagoStrategy implements ValidadorPagoStrategy {

    @Override
    public MedioPago medioSoportado() {
        return MedioPago.TRANSFERENCIA;
    }

    @Override
    public BigDecimal validarYCalcularVuelto(DatosPagoComando pago, BigDecimal totalPedido) {
        if (!Boolean.TRUE.equals(pago.transferenciaVerificada())) {
            throw new ReglaNegocioException(
                    "TRANSFERENCIA_NO_VERIFICADA",
                    "La transferencia por Bre-B debe ser verificada manualmente en Nequi por Caja antes de confirmar (RN-008)"
            );
        }
        if (pago.referenciaExterna() == null || pago.referenciaExterna().isBlank()) {
            throw new ReglaNegocioException(
                    "REFERENCIA_TRANSFERENCIA_OBLIGATORIA",
                    "Debe registrar la referencia externa del movimiento verificado en Nequi (RN-008)"
            );
        }
        return BigDecimal.ZERO.setScale(2);
    }
}
