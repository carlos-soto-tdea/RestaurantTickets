package co.elchupacabras.tickets.shared.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcepcionesYEnumsDominioTest {

    @Test
    @DisplayName("Verifica que EstadoPedido contiene exactamente los 11 estados oficiales de RN-026 y COMP-04")
    void estadoPedidoContieneOnceEstadosOficiales() {
        assertThat(EstadoPedido.values())
                .extracting(Enum::name)
                .containsExactly(
                        "PAGADO_REGISTRADO",
                        "EN_PREPARACION",
                        "LISTO_PARA_DESPACHO",
                        "LISTO_PARA_RECOGER",
                        "EN_RUTA_DOMICILIO",
                        "ENTREGADO",
                        "NO_ENTREGADO",
                        "CANCELADO_REEMBOLSO_PENDIENTE",
                        "REEMBOLSO_PENDIENTE",
                        "REEMBOLSADO",
                        "ENTREGADO_CON_AJUSTE"
                );
    }

    @Test
    @DisplayName("Verifica que ReglaNegocioException y ConcurrenciaInventarioException conservan codigo y detalles")
    void excepcionesDominioConservanCodigoYDetalles() {
        ReglaNegocioException reglaEx = new ReglaNegocioException(
                "STOCK_INSUFICIENTE",
                "No hay carne suficiente para completar la solicitud",
                List.of("Carne al pastor: faltan 200.000 GR")
        );
        assertThat(reglaEx.getCodigo()).isEqualTo("STOCK_INSUFICIENTE");
        assertThat(reglaEx.getDetalles()).containsExactly("Carne al pastor: faltan 200.000 GR");

        ConcurrenciaInventarioException concEx = new ConcurrenciaInventarioException(
                "CONFLICTO_INVENTARIO",
                "El stock cambió durante la confirmación"
        );
        assertThat(concEx.getCodigo()).isEqualTo("CONFLICTO_INVENTARIO");
    }
}
