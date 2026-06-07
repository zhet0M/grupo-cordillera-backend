package com.grupocordillera.kpis.service;

import com.grupocordillera.kpis.dto.KpiResult;
import com.grupocordillera.kpis.enums.KpiEstado;
import com.grupocordillera.kpis.enums.KpiType;
import com.grupocordillera.kpis.model.ProductoKpiDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InventarioKpiCalculator implements KpiCalculator {

    private static final DateTimeFormatter MES_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final KpiDataService dataService;

    @Override
    public boolean supports(KpiType tipo) {
        return switch (tipo) {
            case STOCK_BAJO_MINIMO, ROTACION_INVENTARIO, INVENTARIO_TOTAL_VALOR -> true;
            default -> false;
        };
    }

    @Override
    public KpiResult calcular(KpiType tipo) {
        List<ProductoKpiDTO> productos = dataService.obtenerProductos();
        List<com.grupocordillera.kpis.model.VentaKpiDTO> ventas = dataService.obtenerVentas();
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        return switch (tipo) {
            case STOCK_BAJO_MINIMO -> {
                long bajoStock = productos.stream()
                        .filter(producto -> producto.getStock() != null && producto.getStockMinimo() != null)
                        .filter(producto -> producto.getStock() <= producto.getStockMinimo())
                        .count();
                yield base(tipo, BigDecimal.valueOf(bajoStock), "Productos bajo stock minimo", "Inventario", periodo(formatearMes(inicioMes)), bajoStock > 0 ? KpiEstado.NEGATIVO : KpiEstado.POSITIVO);
            }
            case ROTACION_INVENTARIO -> {
                BigDecimal vendidos = ventas.stream()
                        .filter(venta -> venta.getFecha() != null)
                        .filter(venta -> !venta.getFecha().isBefore(inicioMes) && !venta.getFecha().isAfter(hoy))
                        .map(venta -> venta.getCantidad() == null ? 0 : venta.getCantidad())
                        .map(BigDecimal::valueOf)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal stockActual = productos.stream()
                        .map(producto -> producto.getStock() == null ? 0 : producto.getStock())
                        .map(BigDecimal::valueOf)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal rotacion = stockActual.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : vendidos.divide(stockActual, 2, RoundingMode.HALF_UP);
                yield base(tipo, rotacion, "Rotacion de inventario", "Inventario", periodo(formatearMes(inicioMes)), rotacion.compareTo(BigDecimal.ZERO) > 0 ? KpiEstado.POSITIVO : KpiEstado.SIN_DATOS);
            }
            case INVENTARIO_TOTAL_VALOR -> {
                BigDecimal total = productos.stream()
                        .map(producto -> {
                            double costo = producto.getCosto() == null ? 0d : producto.getCosto();
                            int stock = producto.getStock() == null ? 0 : producto.getStock();
                            return BigDecimal.valueOf(costo).multiply(BigDecimal.valueOf(stock));
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                yield base(tipo, total, "Valor total del inventario a costo", "Inventario", periodo(formatearMes(inicioMes)), total.compareTo(BigDecimal.ZERO) > 0 ? KpiEstado.POSITIVO : KpiEstado.SIN_DATOS);
            }
            default -> throw new IllegalArgumentException("KPI de inventario no soportado: " + tipo);
        };
    }

    private KpiResult base(KpiType tipo, BigDecimal valor, String descripcion, String fuenteDatos, String periodo, KpiEstado estado) {
        KpiResult result = new KpiResult();
        result.setTipo(tipo);
        result.setValor(valor.setScale(2, RoundingMode.HALF_UP));
        result.setVariacion(BigDecimal.ZERO);
        result.setDescripcion(descripcion);
        result.setFuenteDatos(fuenteDatos);
        result.setPeriodo(periodo);
        result.setEstado(estado);
        return result;
    }

    private String formatearMes(LocalDate fecha) {
        return YearMonth.from(fecha).atDay(1).format(MES_FORMAT);
    }

    private String periodo(String valor) {
        return valor;
    }
}
