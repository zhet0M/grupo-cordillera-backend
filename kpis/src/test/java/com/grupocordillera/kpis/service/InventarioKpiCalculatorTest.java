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
import com.grupocordillera.kpis.model.ProductoKpiDTO;
import com.grupocordillera.kpis.model.VentaKpiDTO;

@ExtendWith(MockitoExtension.class)
class InventarioKpiCalculatorTest {

    @Mock
    private KpiDataService dataService;

    @InjectMocks
    private InventarioKpiCalculator calculator;

    private LocalDate hoy;
    private LocalDate inicioMes;

    @BeforeEach
    void setUp() {
        hoy = LocalDate.now();
        inicioMes = hoy.withDayOfMonth(1);
    }

    @Test
    void supportsSoloInventario() {
        assertTrue(calculator.supports(KpiType.STOCK_BAJO_MINIMO));
        assertTrue(calculator.supports(KpiType.ROTACION_INVENTARIO));
        assertTrue(calculator.supports(KpiType.INVENTARIO_TOTAL_VALOR));
        assertFalse(calculator.supports(KpiType.VENTAS_DIA));
        assertFalse(calculator.supports(KpiType.INGRESOS_TOTALES));
    }

    @Test
    void stockBajoMinimoCuentaLosProductosBajos() {
        //HALLAZGO: condición real es `stock <= stockMinimo` (stock IGUAL o menor),
        // NO `stock < stockMinimo`. El helper es producto(id, STOCK, STOCK_MINIMO):
        //   p1: stock=20, min=5  → 20 > 5   → OK, no bajo
        //   p2: stock=2,  min=2  → 2 == 2   → BAJO (por el <=)
        //   p3: stock=1,  min=5  → 1 <= 5   → BAJO
        //   p4: stock=null       → skip (ambos != null condición)
        // Total: 2 bajo (p2+p3)
        ProductoKpiDTO p1 = producto(1L, 20, 5);
        ProductoKpiDTO p2 = producto(2L, 2, 2);
        ProductoKpiDTO p3 = producto(3L, 1, 5);
        ProductoKpiDTO p4 = producto(4L, null, 10);
        when(dataService.obtenerProductos()).thenReturn(List.of(p1, p2, p3, p4));

        KpiResult result = calculator.calcular(KpiType.STOCK_BAJO_MINIMO);

        assertEquals(0, result.getValor().compareTo(new BigDecimal("2")));
        assertEquals(KpiEstado.NEGATIVO, result.getEstado());
    }

    @Test
    void stockBajoMinimoSinProductosBajosDevuelvePositivo() {
        ProductoKpiDTO p1 = producto(1L, 20, 5);
        ProductoKpiDTO p2 = producto(2L, 10, 2);
        when(dataService.obtenerProductos()).thenReturn(List.of(p1, p2));

        KpiResult result = calculator.calcular(KpiType.STOCK_BAJO_MINIMO);

        assertEquals(BigDecimal.ZERO.setScale(2), result.getValor());
        assertEquals(KpiEstado.POSITIVO, result.getEstado());
    }

    @Test
    void rotacionInventarioCalculaVendidosSobreStockActual() {
        ProductoKpiDTO p1 = producto(1L, 10, 2);
        ProductoKpiDTO p2 = producto(2L, 10, 2);
        when(dataService.obtenerProductos()).thenReturn(List.of(p1, p2));

        VentaKpiDTO v1 = venta(1L, inicioMes, 100.0, 3);
        VentaKpiDTO v2 = venta(2L, inicioMes, 200.0, 5);
        when(dataService.obtenerVentas()).thenReturn(List.of(v1, v2));

        // Stock actual = 10 + 10 = 20, vendidos = 3 + 5 = 8 → rotación = 8/20 = 0.40
        KpiResult result = calculator.calcular(KpiType.ROTACION_INVENTARIO);

        assertEquals(new BigDecimal("0.40"), result.getValor());
        assertEquals(KpiEstado.POSITIVO, result.getEstado());
    }

    @Test
    void inventarioTotalValorCalculaSumaCostoPorStock() {
        ProductoKpiDTO p1 = new ProductoKpiDTO();
        p1.setCosto(50.0);
        p1.setStock(10);
        ProductoKpiDTO p2 = new ProductoKpiDTO();
        p2.setCosto(100.0);
        p2.setStock(5);
        when(dataService.obtenerProductos()).thenReturn(List.of(p1, p2));

        KpiResult result = calculator.calcular(KpiType.INVENTARIO_TOTAL_VALOR);

        // 50*10 + 100*5 = 500 + 500 = 1000
        assertEquals(new BigDecimal("1000.00"), result.getValor());
    }

    @Test
    void sinStockRotacionCeroSinDatos() {
        ProductoKpiDTO sinStock = new ProductoKpiDTO();
        sinStock.setStock(0);
        sinStock.setCosto(10.0);
        when(dataService.obtenerProductos()).thenReturn(List.of(sinStock));
        when(dataService.obtenerVentas()).thenReturn(List.of());

        KpiResult result = calculator.calcular(KpiType.ROTACION_INVENTARIO);

        assertEquals(BigDecimal.ZERO.setScale(2), result.getValor());
        assertEquals(KpiEstado.SIN_DATOS, result.getEstado());
    }

    private ProductoKpiDTO producto(Long id, Integer stock, Integer stockMinimo) {
        ProductoKpiDTO p = new ProductoKpiDTO();
        p.setId(id);
        p.setStock(stock);
        p.setStockMinimo(stockMinimo);
        p.setCosto(10.0);
        return p;
    }

    private VentaKpiDTO venta(Long id, LocalDate fecha, double monto, int cantidad) {
        VentaKpiDTO v = new VentaKpiDTO();
        v.setId(id);
        v.setFecha(fecha);
        v.setMontoTotal(monto);
        v.setCantidad(cantidad);
        return v;
    }
}
