package co.elchupacabras.tickets.pedidos.application;

import co.elchupacabras.tickets.catalogo.infrastructure.ProductoInsumoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.ProductoInsumoJpaRepository;
import co.elchupacabras.tickets.catalogo.infrastructure.ProductoJpaEntity;
import co.elchupacabras.tickets.catalogo.infrastructure.ProductoJpaRepository;
import co.elchupacabras.tickets.domicilios.infrastructure.ZonaDomicilioJpaEntity;
import co.elchupacabras.tickets.domicilios.infrastructure.ZonaDomicilioJpaRepository;
import co.elchupacabras.tickets.inventario.infrastructure.InventarioJornadaJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.InventarioJornadaJpaRepository;
import co.elchupacabras.tickets.inventario.infrastructure.JornadaJpaEntity;
import co.elchupacabras.tickets.inventario.infrastructure.JornadaJpaRepository;
import co.elchupacabras.tickets.pedidos.web.SolicitudValidarRequest;
import co.elchupacabras.tickets.pedidos.web.SolicitudValidarResponse;
import co.elchupacabras.tickets.shared.domain.EstadoJornada;
import co.elchupacabras.tickets.shared.domain.ModalidadPedido;
import co.elchupacabras.tickets.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación que cotiza una Solicitud, valida cobertura en Los Patios (RN-028 a RN-030)
 * y comprueba existencias de receta sin reservar inventario (RN-004, RN-014, RN-015, RNF-002).
 */
@Service
public class ValidadorDisponibilidadService {

    private final JornadaJpaRepository jornadaRepository;
    private final ProductoJpaRepository productoRepository;
    private final ProductoInsumoJpaRepository recetaRepository;
    private final InventarioJornadaJpaRepository inventarioRepository;
    private final ZonaDomicilioJpaRepository zonaDomicilioRepository;

    public ValidadorDisponibilidadService(
            JornadaJpaRepository jornadaRepository,
            ProductoJpaRepository productoRepository,
            ProductoInsumoJpaRepository recetaRepository,
            InventarioJornadaJpaRepository inventarioRepository,
            ZonaDomicilioJpaRepository zonaDomicilioRepository
    ) {
        this.jornadaRepository = jornadaRepository;
        this.productoRepository = productoRepository;
        this.recetaRepository = recetaRepository;
        this.inventarioRepository = inventarioRepository;
        this.zonaDomicilioRepository = zonaDomicilioRepository;
    }

    public record ResultadoCotizacionYReceta(
            JornadaJpaEntity jornada,
            ZonaDomicilioJpaEntity zonaDomicilio,
            Map<Long, ProductoJpaEntity> productosPorId,
            Map<Long, BigDecimal> requerimientosPorInsumo,
            BigDecimal subtotal,
            BigDecimal costoDomicilio,
            BigDecimal total
    ) {
    }

    @Transactional(readOnly = true)
    public SolicitudValidarResponse validarSolicitud(SolicitudValidarRequest request) {
        ResultadoCotizacionYReceta cotizacion = calcularRequerimientosYValidarReglas(request);
        return new SolicitudValidarResponse(
                true,
                cotizacion.subtotal(),
                cotizacion.costoDomicilio(),
                cotizacion.total(),
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public ResultadoCotizacionYReceta calcularRequerimientosYValidarReglas(SolicitudValidarRequest request) {
        JornadaJpaEntity jornada = jornadaRepository.findById(request.jornadaId())
                .orElseThrow(() -> new ReglaNegocioException("JORNADA_NO_ENCONTRADA", "La jornada indicada no existe"));

        if (jornada.getEstado() != EstadoJornada.ABIERTA) {
            throw new ReglaNegocioException("JORNADA_CERRADA", "No se pueden procesar solicitudes en una jornada cerrada");
        }

        ZonaDomicilioJpaEntity zona = null;
        BigDecimal costoDomicilio = BigDecimal.ZERO.setScale(2);

        if (request.modalidad() == ModalidadPedido.DOMICILIO) {
            if (request.zonaDomicilioId() == null || request.direccionTexto() == null || request.direccionTexto().isBlank()) {
                throw new ReglaNegocioException(
                        "DATOS_DOMICILIO_INCOMPLETOS",
                        "Un pedido a domicilio exige dirección completa y zona activa dentro de Los Patios (RN-030)"
                );
            }
            zona = zonaDomicilioRepository.findById(request.zonaDomicilioId())
                    .orElseThrow(() -> new ReglaNegocioException("ZONA_NO_ENCONTRADA", "La zona de domicilio no existe"));
            if (!zona.perteneceALosPatiosYEstaActiva()) {
                throw new ReglaNegocioException(
                        "DOMICILIO_FUERA_DE_COBERTURA",
                        "Los domicilios se realizan únicamente en zonas activas dentro de Los Patios (RN-028)"
                );
            }
            costoDomicilio = zona.getTarifa();
        } else if (request.zonaDomicilioId() != null || (request.direccionTexto() != null && !request.direccionTexto().isBlank())) {
            throw new ReglaNegocioException(
                    "MODALIDAD_NO_ADMITE_DOMICILIO",
                    "Solo la modalidad DOMICILIO admite zona y dirección de entrega"
            );
        }

        List<Long> productoIds = request.items().stream()
                .map(SolicitudValidarRequest.ItemSolicitudRequest::productoId)
                .distinct()
                .toList();

        Map<Long, ProductoJpaEntity> productosPorId = productoRepository.findAllById(productoIds).stream()
                .collect(Collectors.toMap(ProductoJpaEntity::getId, Function.identity()));

        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
        for (SolicitudValidarRequest.ItemSolicitudRequest item : request.items()) {
            ProductoJpaEntity prod = productosPorId.get(item.productoId());
            if (prod == null || !prod.isActivo()) {
                throw new ReglaNegocioException(
                        "PRODUCTO_NO_DISPONIBLE",
                        "El producto con ID " + item.productoId() + " no existe o está inactivo"
                );
            }
            subtotal = subtotal.add(prod.getPrecio().multiply(BigDecimal.valueOf(item.cantidad())));
        }

        List<ProductoInsumoJpaEntity> recetas = recetaRepository.findRecetasByProductoIds(productoIds);
        Map<Long, List<ProductoInsumoJpaEntity>> recetasPorProducto = recetas.stream()
                .collect(Collectors.groupingBy(r -> r.getProducto().getId()));

        Map<Long, BigDecimal> requerimientosPorInsumo = new LinkedHashMap<>();
        Map<Long, String> nombreInsumoPorId = new LinkedHashMap<>();

        for (SolicitudValidarRequest.ItemSolicitudRequest item : request.items()) {
            List<ProductoInsumoJpaEntity> componentes = recetasPorProducto.getOrDefault(item.productoId(), List.of());
            if (componentes.isEmpty()) {
                throw new ReglaNegocioException(
                        "PRODUCTO_SIN_RECETA",
                        "El producto '" + productosPorId.get(item.productoId()).getNombre() + "' no tiene receta configurada (RN-014)"
                );
            }
            for (ProductoInsumoJpaEntity comp : componentes) {
                Long insumoId = comp.getInsumo().getId();
                BigDecimal cantItem = comp.getCantidadRequerida().multiply(BigDecimal.valueOf(item.cantidad()));
                requerimientosPorInsumo.merge(insumoId, cantItem, BigDecimal::add);
                nombreInsumoPorId.put(insumoId, comp.getInsumo().getNombre());
            }
        }

        Map<Long, InventarioJornadaJpaEntity> inventarioPorInsumo = inventarioRepository
                .findByJornadaIdAndInsumoIds(jornada.getId(), requerimientosPorInsumo.keySet())
                .stream()
                .collect(Collectors.toMap(ij -> ij.getInsumo().getId(), Function.identity()));

        List<String> faltantes = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : requerimientosPorInsumo.entrySet()) {
            Long insumoId = entry.getKey();
            BigDecimal requerido = entry.getValue();
            InventarioJornadaJpaEntity inv = inventarioPorInsumo.get(insumoId);
            BigDecimal disponible = inv != null ? inv.getCantidadDisponible() : BigDecimal.ZERO;

            if (disponible.compareTo(requerido) < 0) {
                BigDecimal faltante = requerido.subtract(disponible);
                faltantes.add(nombreInsumoPorId.get(insumoId) + ": requerido=" + requerido + ", disponible=" + disponible + ", faltante=" + faltante);
            }
        }

        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException(
                    "STOCK_INSUFICIENTE",
                    "No existen insumos suficientes para completar la solicitud (RN-015)",
                    faltantes
            );
        }

        BigDecimal total = subtotal.add(costoDomicilio);
        return new ResultadoCotizacionYReceta(
                jornada,
                zona,
                productosPorId,
                requerimientosPorInsumo,
                subtotal,
                costoDomicilio,
                total
        );
    }
}
