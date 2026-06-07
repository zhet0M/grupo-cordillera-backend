package com.grupocordillera.kpis.service;

import com.grupocordillera.kpis.dto.KpiResult;
import com.grupocordillera.kpis.enums.KpiEstado;
import com.grupocordillera.kpis.enums.KpiType;
import com.grupocordillera.kpis.model.MovimientoFinancieroKpiDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FinanzasKpiCalculator implements KpiCalculator {

    private static final DateTimeFormatter MES_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final KpiDataService dataService;

    @Override
    public boolean supports(KpiType tipo) {
        return switch (tipo) {
            case INGRESOS_TOTALES, MARGEN_RENTABILIDAD, COSTOS_OPERACIONALES, UTILIDAD_NETA -> true;
            default -> false;
        };
    }

    @Override
    public KpiResult calcular(KpiType tipo) {
        List<MovimientoFinancieroKpiDTO> movimientos = dataService.obtenerMovimientos();
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        BigDecimal ingresos = movimientos.stream()
                .filter(movimiento -> esDelMes(movimiento, inicioMes, hoy))
                .map(MovimientoFinancieroKpiDTO::getIngresos)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costos = movimientos.stream()
                .filter(movimiento -> esDelMes(movimiento, inicioMes, hoy))
                .map(MovimientoFinancieroKpiDTO::getCosto)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal utilidad = ingresos.subtract(costos);
        BigDecimal margen = ingresos.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : utilidad.multiply(BigDecimal.valueOf(100)).divide(ingresos, 2, RoundingMode.HALF_UP);

        return switch (tipo) {
            case INGRESOS_TOTALES -> base(tipo, ingresos, "Ingresos totales del periodo", "Finanzas", periodo(formatearMes(inicioMes)), ingresos.compareTo(BigDecimal.ZERO) > 0 ? KpiEstado.POSITIVO : KpiEstado.SIN_DATOS);
            case MARGEN_RENTABILIDAD -> base(tipo, margen, "Margen de rentabilidad del periodo", "Finanzas", periodo(formatearMes(inicioMes)), margen.compareTo(BigDecimal.ZERO) > 0 ? KpiEstado.POSITIVO : KpiEstado.SIN_DATOS);
            case COSTOS_OPERACIONALES -> base(tipo, costos, "Costos operacionales del periodo", "Finanzas", periodo(formatearMes(inicioMes)), costos.compareTo(BigDecimal.ZERO) > 0 ? KpiEstado.NEGATIVO : KpiEstado.SIN_DATOS);
            case UTILIDAD_NETA -> base(tipo, utilidad, "Utilidad neta del periodo", "Finanzas", periodo(formatearMes(inicioMes)), utilidad.compareTo(BigDecimal.ZERO) > 0 ? KpiEstado.POSITIVO : KpiEstado.NEGATIVO);
            default -> throw new IllegalArgumentException("KPI financiero no soportado: " + tipo);
        };
    }

    private boolean esDelMes(MovimientoFinancieroKpiDTO movimiento, LocalDate inicio, LocalDate fin) {
        return movimiento.getFechaRegistro() != null
                && !movimiento.getFechaRegistro().isBefore(inicio)
                && !movimiento.getFechaRegistro().isAfter(fin);
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
