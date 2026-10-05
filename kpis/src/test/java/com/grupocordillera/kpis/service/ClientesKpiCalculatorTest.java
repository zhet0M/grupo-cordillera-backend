package com.grupocordillera.kpis.service;

import java.math.BigDecimal;
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
import com.grupocordillera.kpis.model.ClienteKpiDTO;

@ExtendWith(MockitoExtension.class)
class ClientesKpiCalculatorTest {

    @Mock
    private KpiDataService dataService;

    @InjectMocks
    private ClientesKpiCalculator calculator;

    private LocalDate hoy;
    private LocalDate inicioMes;

    @BeforeEach
    void setUp() {
        hoy = LocalDate.now();
        inicioMes = hoy.withDayOfMonth(1);
    }

    @Test
    void supportsSoloClientes() {
        assertTrue(calculator.supports(KpiType.CLIENTES_NUEVOS));
        assertTrue(calculator.supports(KpiType.CLIENTES_FRECUENTES));
        assertFalse(calculator.supports(KpiType.VENTAS_DIA));
        assertFalse(calculator.supports(KpiType.STOCK_BAJO_MINIMO));
        assertFalse(calculator.supports(KpiType.INGRESOS_TOTALES));
    }

    @Test
    void clientesNuevosCuentaLosRegistradosEsteMes() {
        ClienteKpiDTO cNuevo1 = cliente(1L, inicioMes, 1);
        ClienteKpiDTO cNuevo2 = cliente(2L, hoy, 0);
        ClienteKpiDTO cViejo = cliente(3L, inicioMes.minusMonths(1), 10);
        when(dataService.obtenerClientes()).thenReturn(List.of(cNuevo1, cNuevo2, cViejo));

        KpiResult result = calculator.calcular(KpiType.CLIENTES_NUEVOS);

        assertEquals(new BigDecimal("2"), result.getValor());
        assertEquals(KpiEstado.POSITIVO, result.getEstado());
    }

    @Test
    void clientesFrecuentesCuentaLosDeMasDeUnaCompraYAgrupaDetalles() {
        ClienteKpiDTO cFrecuente1 = cliente(1L, inicioMes.minusMonths(2), 5);
        ClienteKpiDTO cFrecuente2 = cliente(2L, inicioMes.minusMonths(1), 3);
        ClienteKpiDTO cNuevo = cliente(3L, inicioMes.plusDays(2), 1);   // frecuentes=NO
        ClienteKpiDTO cNuevo2 = cliente(4L, inicioMes.plusDays(3), 0);  // frecuentes=NO
        ClienteKpiDTO cViejo = cliente(5L, inicioMes.minusMonths(3), 1);
        when(dataService.obtenerClientes()).thenReturn(List.of(cFrecuente1, cFrecuente2, cNuevo, cNuevo2, cViejo));

        KpiResult result = calculator.calcular(KpiType.CLIENTES_FRECUENTES);

        assertEquals(new BigDecimal("2"), result.getValor());
        assertNotNull(result.getDetalles());
        // FRECUENTES = 2 (los con >1 compra), NUEVOS = 2 (registrados en mes actual)
        assertEquals(new BigDecimal("2"), result.getDetalles().get("FRECUENTES"));
        assertEquals(new BigDecimal("2"), result.getDetalles().get("NUEVOS"));
    }

    @Test
    void sinClientesNuevosRetornaSinDatos() {
        ClienteKpiDTO cViejo = cliente(1L, inicioMes.minusYears(1), 20);
        when(dataService.obtenerClientes()).thenReturn(List.of(cViejo));

        KpiResult result = calculator.calcular(KpiType.CLIENTES_NUEVOS);

        assertEquals(BigDecimal.ZERO, result.getValor());
        assertEquals(KpiEstado.SIN_DATOS, result.getEstado());
    }

    private ClienteKpiDTO cliente(Long id, LocalDate fechaRegistro, int cantidadCompras) {
        ClienteKpiDTO c = new ClienteKpiDTO();
        c.setId(id);
        c.setFechaRegistro(fechaRegistro);
        c.setCantidadCompras(cantidadCompras);
        c.setMontoAcumulado(BigDecimal.ZERO);
        return c;
    }
}
