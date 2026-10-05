package com.grupocordillera.kpis.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.grupocordillera.kpis.dto.KpiResult;
import com.grupocordillera.kpis.enums.KpiEstado;
import com.grupocordillera.kpis.enums.KpiType;
import com.grupocordillera.kpis.model.MovimientoFinancieroKpiDTO;

@ExtendWith(MockitoExtension.class)
class FinanzasKpiCalculatorTest {

    @Mock
    private KpiDataService dataService;

    @InjectMocks
    private FinanzasKpiCalculator calculator;

    private LocalDate hoy;
    private LocalDate inicioMes;

    @BeforeEach
    void setUp() {
        hoy = LocalDate.now();
        inicioMes = hoy.withDayOfMonth(1);
    }

    @Test
    void supportsSoloFinanzas() {
        assertTrue(calculator.supports(KpiType.INGRESOS_TOTALES));
        assertTrue(calculator.supports(KpiType.MARGEN_RENTABILIDAD));
        assertTrue(calculator.supports(KpiType.COSTOS_OPERACIONALES));
        assertTrue(calculator.supports(KpiType.UTILIDAD_NETA));
        assertFalse(calculator.supports(KpiType.VENTAS_DIA));
        assertFalse(calculator.supports(KpiType.STOCK_BAJO_MINIMO));
    }

    @Test
    void ingresosTotalesSumaSoloLosDelMes() {
        MovimientoFinancieroKpiDTO m1 = movimiento(inicioMes, new BigDecimal("5000"), new BigDecimal("3000"), new BigDecimal("2000"));
        //HALLAZGO: el filtro `esDelMes` usa rango [inicioMes, HOY] (no todo el mes).
        // Si hoy es 4 de octubre, plusDays(5) = 9 de octubre → cae EN EL FUTURO → se descarta.
        // Usamos plusDays(1) = 2 de octubre, que seguro está <= hoy.
        MovimientoFinancieroKpiDTO m2 = movimiento(inicioMes.plusDays(1), new BigDecimal("3000"), new BigDecimal("1000"), new BigDecimal("2000"));
        MovimientoFinancieroKpiDTO mFuera = movimiento(inicioMes.minusDays(1), new BigDecimal("9999"), new BigDecimal("9999"), new BigDecimal("0"));
        when(dataService.obtenerMovimientos()).thenReturn(List.of(m1, m2, mFuera));

        KpiResult result = calculator.calcular(KpiType.INGRESOS_TOTALES);

        assertEquals(0, result.getValor().compareTo(new BigDecimal("8000")));
        assertEquals(KpiEstado.POSITIVO, result.getEstado());
    }

    @Test
    void margenRentabilidadCalculaPorcentajeCorrecto() {
        // Ingresos = 10000, costos = 7000, utilidad = 3000, margen = 30%
        MovimientoFinancieroKpiDTO m1 = movimiento(inicioMes, new BigDecimal("10000"), new BigDecimal("7000"), new BigDecimal("3000"));
        when(dataService.obtenerMovimientos()).thenReturn(List.of(m1));

        KpiResult result = calculator.calcular(KpiType.MARGEN_RENTABILIDAD);

        assertEquals(new BigDecimal("30.00"), result.getValor());
    }

    @Test
    void utilidadNetaCalculaIngresosMenosCostos() {
        MovimientoFinancieroKpiDTO m1 = movimiento(inicioMes, new BigDecimal("5000"), new BigDecimal("3500"), null);
        MovimientoFinancieroKpiDTO m2 = movimiento(inicioMes, new BigDecimal("2000"), new BigDecimal("1000"), null);
        when(dataService.obtenerMovimientos()).thenReturn(List.of(m1, m2));

        // Ingresos 7000 - Costos 4500 = 2500
        KpiResult result = calculator.calcular(KpiType.UTILIDAD_NETA);

        assertEquals(new BigDecimal("2500.00"), result.getValor());
        assertEquals(KpiEstado.POSITIVO, result.getEstado());
    }

    @Test
    void utilidadNetaNegativaMarcaEstadoNegativo() {
        MovimientoFinancieroKpiDTO m1 = movimiento(inicioMes, new BigDecimal("1000"), new BigDecimal("3000"), new BigDecimal("-2000"));
        when(dataService.obtenerMovimientos()).thenReturn(List.of(m1));

        KpiResult result = calculator.calcular(KpiType.UTILIDAD_NETA);

        assertEquals(new BigDecimal("-2000.00"), result.getValor());
        assertEquals(KpiEstado.NEGATIVO, result.getEstado());
    }

    @Test
    void sinDatosRetornaSinDatos() {
        when(dataService.obtenerMovimientos()).thenReturn(List.of());

        KpiResult ingresos = calculator.calcular(KpiType.INGRESOS_TOTALES);
        assertEquals(BigDecimal.ZERO.setScale(2), ingresos.getValor());
        assertEquals(KpiEstado.SIN_DATOS, ingresos.getEstado());

        KpiResult margen = calculator.calcular(KpiType.MARGEN_RENTABILIDAD);
        assertEquals(BigDecimal.ZERO.setScale(2), margen.getValor());
        assertEquals(KpiEstado.SIN_DATOS, margen.getEstado());
    }

    private MovimientoFinancieroKpiDTO movimiento(LocalDate fecha, BigDecimal ingresos, BigDecimal costo, BigDecimal margen) {
        MovimientoFinancieroKpiDTO m = new MovimientoFinancieroKpiDTO();
        m.setFechaRegistro(fecha);
        m.setIngresos(ingresos);
        m.setCosto(costo);
        m.setMargen(margen);
        return m;
    }
}
