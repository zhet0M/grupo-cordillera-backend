package com.grupocordillera.kpis.service;

import com.grupocordillera.kpis.dto.KpiResult;
import com.grupocordillera.kpis.enums.KpiEstado;
import com.grupocordillera.kpis.enums.KpiType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KpiServiceTest {

    @Mock
    private KpiFactory kpiFactory;

    @Mock
    private KpiCalculator mockCalculator;

    @InjectMocks
    private KpiService kpiService;

    @Test
    void obtenerResumenRetornaUnKpiPorCadaTipo() {
        when(kpiFactory.crear(any())).thenReturn(mockCalculator);
        when(mockCalculator.calcular(any())).thenAnswer(invocation -> {
            KpiType tipo = invocation.getArgument(0);
            KpiResult r = new KpiResult();
            r.setTipo(tipo);
            r.setValor(BigDecimal.TEN);
            r.setEstado(KpiEstado.POSITIVO);
            return r;
        });

        List<KpiResult> resultado = kpiService.obtenerResumen();

        assertEquals(KpiType.values().length, resultado.size());
        for (KpiType tipo : KpiType.values()) {
            long count = resultado.stream().filter(r -> r.getTipo() == tipo).count();
            assertEquals(1, count, "Debe haber un resultado para el tipo " + tipo);
        }
    }

    @Test
    void obtenerPorTipoPropagaElResultadoDelCalculator() {
        KpiResult esperado = new KpiResult();
        esperado.setTipo(KpiType.VENTAS_DIA);
        esperado.setValor(new BigDecimal("5000"));
        esperado.setEstado(KpiEstado.POSITIVO);

        when(kpiFactory.crear(KpiType.VENTAS_DIA)).thenReturn(mockCalculator);
        when(mockCalculator.calcular(KpiType.VENTAS_DIA)).thenReturn(esperado);

        KpiResult resultado = kpiService.obtenerPorTipo(KpiType.VENTAS_DIA);

        assertSame(esperado, resultado);
    }

    @Test
    void obtenerPorTipoSiCalculatorFallaRetornaResultadoDeErrorSinDatos() {
        when(kpiFactory.crear(KpiType.VENTAS_MES)).thenThrow(new RuntimeException("Servicio caído"));

        KpiResult resultado = kpiService.obtenerPorTipo(KpiType.VENTAS_MES);

        assertNotNull(resultado);
        assertEquals(KpiType.VENTAS_MES, resultado.getTipo());
        assertEquals(BigDecimal.ZERO, resultado.getValor());
        assertEquals(KpiEstado.SIN_DATOS, resultado.getEstado());
        assertEquals("ERROR", resultado.getFuenteDatos());
        assertEquals("No fue posible calcular este KPI en este momento", resultado.getDescripcion());
    }
}
