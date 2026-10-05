package com.grupocordillera.kpis.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.grupocordillera.kpis.model.VentaKpiDTO;

@ExtendWith(MockitoExtension.class)
class VentasKpiCalculatorTest {

    @Mock
    private KpiDataService dataService;

    @InjectMocks
    private VentasKpiCalculator calculator;

    private LocalDate hoy;
    private LocalDate inicioMes;
    private LocalDate inicioSemana;

    @BeforeEach
    void setUp() {
        hoy = LocalDate.now();
        inicioMes = hoy.withDayOfMonth(1);
        inicioSemana = hoy.with(DayOfWeek.MONDAY);
    }

    @Test
    void supportsDevuelveTrueSoloParaVentas() {
        assertTrue(calculator.supports(KpiType.VENTAS_DIA));
        assertTrue(calculator.supports(KpiType.VENTAS_SEMANA));
        assertTrue(calculator.supports(KpiType.VENTAS_MES));
        assertTrue(calculator.supports(KpiType.VENTAS_POR_SUCURSAL));
        assertTrue(calculator.supports(KpiType.VENTAS_POR_CANAL));
        assertTrue(calculator.supports(KpiType.TICKET_PROMEDIO));
        assertTrue(calculator.supports(KpiType.VARIACION_MENSUAL));
        assertTrue(calculator.supports(KpiType.SUCURSAL_MEJOR_RENDIMIENTO));
        assertFalse(calculator.supports(KpiType.STOCK_BAJO_MINIMO));
        assertFalse(calculator.supports(KpiType.INGRESOS_TOTALES));
        assertFalse(calculator.supports(KpiType.CLIENTES_NUEVOS));
    }

    @Test
    void ventasDiaSumaSoloVentasDelDia() {
        VentaKpiDTO vHoy = venta(1L, hoy, 500.0, "SUC-CENTRAL", "POS");
        VentaKpiDTO vAyer = venta(2L, hoy.minusDays(1), 300.0, "SUC-CENTRAL", "POS");
        when(dataService.obtenerVentas()).thenReturn(List.of(vHoy, vAyer));

        KpiResult result = calculator.calcular(KpiType.VENTAS_DIA);

        assertEquals(new BigDecimal("500.00"), result.getValor());
        assertEquals(KpiEstado.POSITIVO, result.getEstado());
        assertEquals("Ventas", result.getFuenteDatos());
    }

    @Test
    void ventasSemanaSumaSoloVentasDeLaSemana() {
        VentaKpiDTO vSemana = venta(1L, inicioSemana, 200.0, "SUC-NORTE", "ECOMMERCE");
        VentaKpiDTO vPrevia = venta(2L, inicioSemana.minusDays(1), 999.0, "SUC-NORTE", "POS");
        when(dataService.obtenerVentas()).thenReturn(List.of(vSemana, vPrevia));

        KpiResult result = calculator.calcular(KpiType.VENTAS_SEMANA);

        assertEquals(new BigDecimal("200.00"), result.getValor());
    }

    @Test
    void ventasPorSucursalRetornaElMaximoYDetallaPorSucursal() {
        VentaKpiDTO vCentral = venta(1L, inicioMes, 1000.0, "SUC-CENTRAL", "POS");
        VentaKpiDTO vCentral2 = venta(2L, inicioMes, 500.0, "SUC-CENTRAL", "POS");
        VentaKpiDTO vNorte = venta(3L, inicioMes, 300.0, "SUC-NORTE", "ECOMMERCE");
        when(dataService.obtenerVentas()).thenReturn(List.of(vCentral, vCentral2, vNorte));

        KpiResult result = calculator.calcular(KpiType.VENTAS_POR_SUCURSAL);

        //HALLAZGO: BigDecimal.setScale(2) retorna escala 2 solo si el valor
        // requiere precisión, pero el stream de Collectors.reducing() puede
        // producir escalas dispares. Usamos compareTo (valor numérico) en vez
        // de equals, que compara también escala.
        assertEquals(0, result.getValor().compareTo(new BigDecimal("1500.00")));
        assertNotNull(result.getDetalles());
        assertEquals(0, result.getDetalles().get("SUC-CENTRAL").compareTo(new BigDecimal("1500")));
        assertEquals(0, result.getDetalles().get("SUC-NORTE").compareTo(new BigDecimal("300")));
    }

    @Test
    void ticketPromedioCalculaPromedioCorrecto() {
        VentaKpiDTO v1 = venta(1L, inicioMes, 100.0, "A", "POS");
        VentaKpiDTO v2 = venta(2L, inicioMes, 200.0, "A", "POS");
        VentaKpiDTO v3 = venta(3L, inicioMes, 300.0, "A", "POS");
        when(dataService.obtenerVentas()).thenReturn(List.of(v1, v2, v3));

        KpiResult result = calculator.calcular(KpiType.TICKET_PROMEDIO);

        assertEquals(new BigDecimal("200.00"), result.getValor());
    }

    @Test
    void variacionMensualCalculaPorcentajeCorrecto() {
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1).withDayOfMonth(1);
        LocalDate finMesAnterior = inicioMes.minusDays(1);

        VentaKpiDTO actual = venta(1L, inicioMes, 1200.0, "A", "POS");
        VentaKpiDTO anterior = venta(2L, inicioMesAnterior, 1000.0, "A", "POS");
        when(dataService.obtenerVentas()).thenReturn(List.of(actual, anterior));

        KpiResult result = calculator.calcular(KpiType.VARIACION_MENSUAL);

        assertEquals(new BigDecimal("1200.00"), result.getValor());
        // Δ% = (1200 - 1000) * 100 / 1000 = 20.00%
        assertEquals(new BigDecimal("20.00"), result.getVariacion());
    }

    @Test
    void sucursalMejorRendimientoRetornaLaMayor() {
        VentaKpiDTO vCentral = venta(1L, inicioMes, 5000.0, "SUC-CENTRAL", "POS");
        VentaKpiDTO vNorte = venta(2L, inicioMes, 9000.0, "SUC-NORTE", "POS");
        VentaKpiDTO vEste = venta(3L, inicioMes, 3000.0, "SUC-ESTE", "POS");
        when(dataService.obtenerVentas()).thenReturn(List.of(vCentral, vNorte, vEste));

        KpiResult result = calculator.calcular(KpiType.SUCURSAL_MEJOR_RENDIMIENTO);

        //HALLAZGO: compareTo por valor numérico, equals por valor+escala
        assertEquals(0, result.getValor().compareTo(new BigDecimal("9000")));
        assertTrue(result.getDetalles().containsKey("SUC-NORTE"));
        assertEquals(0, result.getDetalles().get("SUC-NORTE").compareTo(new BigDecimal("9000")));
    }

    @Test
    void sinVentasRetornaSinDatos() {
        when(dataService.obtenerVentas()).thenReturn(List.of());

        KpiResult ticketPromedio = calculator.calcular(KpiType.TICKET_PROMEDIO);
        assertEquals(BigDecimal.ZERO.setScale(2), ticketPromedio.getValor());
        assertEquals(KpiEstado.SIN_DATOS, ticketPromedio.getEstado());

        KpiResult porSucursal = calculator.calcular(KpiType.VENTAS_POR_SUCURSAL);
        assertEquals(KpiEstado.SIN_DATOS, porSucursal.getEstado());
    }

    private VentaKpiDTO venta(Long id, LocalDate fecha, double monto, String sucursal, String canal) {
        VentaKpiDTO v = new VentaKpiDTO();
        v.setId(id);
        v.setFecha(fecha);
        v.setMontoTotal(monto);
        v.setCantidad(1);
        v.setSucursal(sucursal);
        v.setCanal(canal);
        return v;
    }
}
