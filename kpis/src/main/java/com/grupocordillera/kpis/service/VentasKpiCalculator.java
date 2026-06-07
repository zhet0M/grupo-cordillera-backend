package com.grupocordillera.kpis.service;

import com.grupocordillera.kpis.dto.KpiResult;
import com.grupocordillera.kpis.enums.KpiEstado;
import com.grupocordillera.kpis.enums.KpiType;
import com.grupocordillera.kpis.model.VentaKpiDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VentasKpiCalculator implements KpiCalculator {

    private static final DateTimeFormatter MES_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final KpiDataService dataService;

    @Override
    public boolean supports(KpiType tipo) {
        return switch (tipo) {
            case VENTAS_DIA, VENTAS_SEMANA, VENTAS_MES, VENTAS_POR_SUCURSAL,
                 VENTAS_POR_CANAL, TICKET_PROMEDIO, VARIACION_MENSUAL,
                 SUCURSAL_MEJOR_RENDIMIENTO -> true;
            default -> false;
        };
    }

    @Override
    public KpiResult calcular(KpiType tipo) {
        List<VentaKpiDTO> ventas = dataService.obtenerVentas();
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate inicioSemana = hoy.with(DayOfWeek.MONDAY);
        LocalDate finMesAnterior = inicioMes.minusDays(1);
        LocalDate inicioMesAnterior = finMesAnterior.withDayOfMonth(1);

        return switch (tipo) {
            case VENTAS_DIA -> resultadoVentas(
                    tipo,
                    sumarVentasFiltradas(ventas, hoy, hoy),
                    BigDecimal.ZERO,
                    "Ventas del dia",
                    "Ventas",
                    periodo("Hoy"),
                    KpiEstado.POSITIVO
            );
            case VENTAS_SEMANA -> resultadoVentas(
                    tipo,
                    sumarVentasFiltradas(ventas, inicioSemana, hoy),
                    BigDecimal.ZERO,
                    "Ventas de la semana actual",
                    "Ventas",
                    periodo("Semana actual"),
                    KpiEstado.POSITIVO
            );
            case VENTAS_MES -> resultadoVentas(
                    tipo,
                    sumarVentasFiltradas(ventas, inicioMes, hoy),
                    BigDecimal.ZERO,
                    "Ventas totales del mes",
                    "Ventas",
                    periodo(formatearMes(inicioMes)),
                    KpiEstado.POSITIVO
            );
            case VENTAS_POR_SUCURSAL -> {
                Map<String, BigDecimal> porSucursal = agruparPor(ventas, inicioMes, hoy, VentaKpiDTO::getSucursal);
                BigDecimal max = maximo(porSucursal);
                KpiResult result = resultadoVentas(
                        tipo,
                        max,
                        BigDecimal.ZERO,
                        "Ventas consolidadas por sucursal",
                        "Ventas",
                        periodo(formatearMes(inicioMes)),
                        porSucursal.isEmpty() ? KpiEstado.SIN_DATOS : KpiEstado.POSITIVO
                );
                result.setDetalles(porSucursal);
                yield result;
            }
            case VENTAS_POR_CANAL -> {
                Map<String, BigDecimal> porCanal = agruparPor(ventas, inicioMes, hoy, venta -> normalizarCanal(venta.getCanal()));
                BigDecimal max = maximo(porCanal);
                KpiResult result = resultadoVentas(
                        tipo,
                        max,
                        BigDecimal.ZERO,
                        "Ventas consolidadas por canal",
                        "Ventas",
                        periodo(formatearMes(inicioMes)),
                        porCanal.isEmpty() ? KpiEstado.SIN_DATOS : KpiEstado.POSITIVO
                );
                result.setDetalles(porCanal);
                yield result;
            }
            case TICKET_PROMEDIO -> {
                List<VentaKpiDTO> ventasMes = filtrar(ventas, inicioMes, hoy);
                BigDecimal total = sumarMonto(ventasMes);
                BigDecimal promedio = ventasMes.isEmpty()
                        ? BigDecimal.ZERO
                        : total.divide(BigDecimal.valueOf(ventasMes.size()), 2, RoundingMode.HALF_UP);
                yield resultadoVentas(
                        tipo,
                        promedio,
                        BigDecimal.ZERO,
                        "Promedio de ticket por venta",
                        "Ventas",
                        periodo(formatearMes(inicioMes)),
                        ventasMes.isEmpty() ? KpiEstado.SIN_DATOS : KpiEstado.POSITIVO
                );
            }
            case VARIACION_MENSUAL -> {
                BigDecimal actual = sumarVentasFiltradas(ventas, inicioMes, hoy);
                BigDecimal anterior = sumarVentasFiltradas(ventas, inicioMesAnterior, finMesAnterior);
                BigDecimal variacion = porcentajeCambio(actual, anterior);
                KpiResult result = resultadoVentas(
                        tipo,
                        actual,
                        variacion,
                        "Variacion frente al mes anterior",
                        "Ventas",
                        periodo(formatearMes(inicioMes)),
                        actual.compareTo(BigDecimal.ZERO) > 0 ? KpiEstado.POSITIVO : KpiEstado.SIN_DATOS
                );
                yield result;
            }
            case SUCURSAL_MEJOR_RENDIMIENTO -> {
                Map<String, BigDecimal> porSucursal = agruparPor(ventas, inicioMes, hoy, VentaKpiDTO::getSucursal);
                Map.Entry<String, BigDecimal> mejor = porSucursal.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .orElse(null);
                KpiResult result = resultadoVentas(
                        tipo,
                        mejor == null ? BigDecimal.ZERO : mejor.getValue(),
                        BigDecimal.ZERO,
                        mejor == null ? "Sin datos de sucursales" : "Sucursal con mejor rendimiento",
                        "Ventas",
                        periodo(formatearMes(inicioMes)),
                        mejor == null ? KpiEstado.SIN_DATOS : KpiEstado.POSITIVO
                );
                if (mejor != null) {
                    result.getDetalles().put(mejor.getKey(), mejor.getValue());
                }
                yield result;
            }
            default -> throw new IllegalArgumentException("KPI de ventas no soportado: " + tipo);
        };
    }

    private KpiResult resultadoVentas(KpiType tipo, BigDecimal valor, BigDecimal variacion, String descripcion, String fuenteDatos, String periodo, KpiEstado estado) {
        KpiResult result = new KpiResult();
        result.setTipo(tipo);
        result.setValor(valor.setScale(2, RoundingMode.HALF_UP));
        result.setVariacion(variacion.setScale(2, RoundingMode.HALF_UP));
        result.setDescripcion(descripcion);
        result.setFuenteDatos(fuenteDatos);
        result.setPeriodo(periodo);
        result.setEstado(estado);
        return result;
    }

    private List<VentaKpiDTO> filtrar(List<VentaKpiDTO> ventas, LocalDate inicio, LocalDate fin) {
        return ventas.stream()
                .filter(venta -> venta.getFecha() != null)
                .filter(venta -> !venta.getFecha().isBefore(inicio) && !venta.getFecha().isAfter(fin))
                .toList();
    }

    private BigDecimal sumarVentasFiltradas(List<VentaKpiDTO> ventas, LocalDate inicio, LocalDate fin) {
        return sumarMonto(filtrar(ventas, inicio, fin));
    }

    private BigDecimal sumarMonto(List<VentaKpiDTO> ventas) {
        return ventas.stream()
                .map(VentaKpiDTO::getMontoTotal)
                .filter(valor -> valor != null)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, BigDecimal> agruparPor(List<VentaKpiDTO> ventas, LocalDate inicio, LocalDate fin, Function<VentaKpiDTO, String> extractor) {
        return filtrar(ventas, inicio, fin).stream()
                .collect(Collectors.groupingBy(
                        venta -> normalizarClave(extractor.apply(venta)),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, venta -> BigDecimal.valueOf(venta.getMontoTotal() == null ? 0d : venta.getMontoTotal()), BigDecimal::add)
                ));
    }

    private BigDecimal maximo(Map<String, BigDecimal> mapa) {
        return mapa.values().stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    }

    private BigDecimal porcentajeCambio(BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return actual.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return actual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior, 2, RoundingMode.HALF_UP);
    }

    private String formatearMes(LocalDate fecha) {
        return YearMonth.from(fecha).atDay(1).format(MES_FORMAT);
    }

    private String periodo(String periodo) {
        return periodo;
    }

    private String normalizarCanal(String canal) {
        if (canal == null || canal.isBlank()) {
            return "SIN_CANAL";
        }
        return canal.trim().toUpperCase();
    }

    private String normalizarClave(String valor) {
        if (valor == null || valor.isBlank()) {
            return "SIN_DATO";
        }
        return valor.trim();
    }
}
