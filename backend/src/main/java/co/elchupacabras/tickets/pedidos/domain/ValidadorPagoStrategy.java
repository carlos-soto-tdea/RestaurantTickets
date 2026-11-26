package co.elchupacabras.tickets.pedidos.domain;

import co.elchupacabras.tickets.shared.domain.MedioPago;

import java.math.BigDecimal;

/**
 * Patrón Strategy + Principio Abierto/Cerrado (OCP - SOLID):
 * Define el contrato polimórfico para validar y calcular condiciones de pago según el medio (RN-003, RN-008)
 * sin acoplar el servicio transaccional a bloques condicionales rígidos.
 */
public interface ValidadorPagoStrategy {

    MedioPago medioSoportado();

    BigDecimal validarYCalcularVuelto(DatosPagoComando pago, BigDecimal totalPedido);
}
