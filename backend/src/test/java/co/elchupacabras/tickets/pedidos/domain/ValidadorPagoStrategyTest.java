package co.elchupacabras.tickets.pedidos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorPagoStrategyTest {

    private final EfectivoPagoStrategy efectivoStrategy = new EfectivoPagoStrategy();
    private final TransferenciaNequiPagoStrategy transferenciaStrategy = new TransferenciaNequiPagoStrategy();

    @Test
    @DisplayName("RN-003: Pago en efectivo calcula correctamente el vuelto cuando montoRecibido >= total")
    void efectivoCalculaVueltoCorrectamente() {
        DatosPagoComando datos = new DatosPagoComando(
                co.elchupacabras.tickets.shared.domain.MedioPago.EFECTIVO,
                new BigDecimal("30000.00"),
                null,
                null
        );
        BigDecimal vuelto = efectivoStrategy.validarYCalcularVuelto(datos, new BigDecimal("24000.00"));
        assertThat(vuelto).isEqualByComparingTo("6000.00");
    }

    @Test
    @DisplayName("RN-003: Pago en efectivo con montoRecibido menor al total lanza ReglaNegocioException (422)")
    void efectivoInsuficienteLanzaExcepcion422() {
        DatosPagoComando datos = new DatosPagoComando(
                co.elchupacabras.tickets.shared.domain.MedioPago.EFECTIVO,
                new BigDecimal("20000.00"),
                null,
                null
        );
        assertThatThrownBy(() -> efectivoStrategy.validarYCalcularVuelto(datos, new BigDecimal("24000.00")))
                .isInstanceOf(co.elchupacabras.tickets.shared.domain.ReglaNegocioException.class)
                .hasMessageContaining("efectivo recibido es menor");
    }

    @Test
    @DisplayName("RN-008: Transferencia Nequi sin verificación manual por Caja lanza ReglaNegocioException (422)")
    void transferenciaNoVerificadaLanzaExcepcion422() {
        DatosPagoComando datos = new DatosPagoComando(
                co.elchupacabras.tickets.shared.domain.MedioPago.TRANSFERENCIA,
                null,
                false,
                "NEQUI-REF-123"
        );
        assertThatThrownBy(() -> transferenciaStrategy.validarYCalcularVuelto(datos, new BigDecimal("24000.00")))
                .isInstanceOf(co.elchupacabras.tickets.shared.domain.ReglaNegocioException.class)
                .hasMessageContaining("verificada manualmente en Nequi");
    }

    @Test
    @DisplayName("RN-008: Transferencia Nequi verificada y con referencia externa es aceptada con vuelto 0")
    void transferenciaVerificadaConReferenciaEsAceptada() {
        DatosPagoComando datos = new DatosPagoComando(
                co.elchupacabras.tickets.shared.domain.MedioPago.TRANSFERENCIA,
                null,
                true,
                "BREB-998877"
        );
        BigDecimal vuelto = transferenciaStrategy.validarYCalcularVuelto(datos, new BigDecimal("24000.00"));
        assertThat(vuelto).isEqualByComparingTo("0.00");
    }
}
