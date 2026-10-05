package com.grupocordillera.reportes.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.grupocordillera.reportes.client.FinanzasClient;
import com.grupocordillera.reportes.client.InventarioClient;
import com.grupocordillera.reportes.client.VentasClient;
import com.grupocordillera.reportes.dto.MovimientoFinancieroReporteDTO;
import com.grupocordillera.reportes.dto.ProductoReporteDTO;
import com.grupocordillera.reportes.dto.ResumenFinancieroDTO;
import com.grupocordillera.reportes.dto.VentaReporteDTO;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private VentasClient ventasClient;

    @Mock
    private InventarioClient inventarioClient;

    @Mock
    private FinanzasClient finanzasClient;

    @InjectMocks
    private ReporteService reporteService;

    private LocalDate hoy;
    private LocalDate inicioMes;

    @BeforeEach
    void setUp() {
        hoy = LocalDate.now();
        inicioMes = hoy.withDayOfMonth(1);
    }

    // ==================== RF-6.2 Reporte de VENTAS ====================

    @Test
    void generarReporteVentasConDatosValidosRetornaPdfNoVacio() {
        VentaReporteDTO v1 = new VentaReporteDTO();
        v1.setId(1L);
        v1.setFecha(hoy.minusDays(1));
        v1.setSucursal("SUC-CENTRAL");
        v1.setNombreProducto("Notebook");
        v1.setCantidad(1);
        v1.setMontoTotal(1000.0);
        v1.setEstadoFinanzas("SINCRONIZADO");

        VentaReporteDTO v2 = new VentaReporteDTO();
        v2.setId(2L);
        v2.setFecha(hoy);
        v2.setSucursal("SUC-NORTE");
        v2.setNombreProducto("Celular");
        v2.setCantidad(2);
        v2.setMontoTotal(500.0);
        v2.setEstadoFinanzas("PENDIENTE");

        when(ventasClient.obtenerPorRango(inicioMes, hoy)).thenReturn(List.of(v1, v2));

        byte[] pdf = reporteService.generarReporteVentas(inicioMes, hoy);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        assertPdfHeader(pdf);
        verificaLlamadaVentas(inicioMes, hoy);
    }

    @Test
    void generarReporteVentasSinDatosRetornaPdfConHeaderSolo() {
        when(ventasClient.obtenerPorRango(inicioMes, hoy)).thenReturn(List.of());

        byte[] pdf = assertDoesNotThrow(() -> reporteService.generarReporteVentas(inicioMes, hoy));

        assertNotNull(pdf);
        assertTrue(pdf.length > 50);
        assertPdfHeader(pdf);
    }

    @Test
    void generarReporteVentasConRangoInvertidoIntercambiaInicioYFin() {
        // HALLAZGO: el service SWAPEA silenciosamente rango invertido (inicio > fin)
        // usando resolverRango(). No lanza error.
        VentaReporteDTO v = new VentaReporteDTO();
        v.setId(1L);
        v.setFecha(hoy);
        v.setMontoTotal(250.0);

        // esperamos que el service use (inicioMes, hoy) y no (hoy, inicioMes)
        when(ventasClient.obtenerPorRango(inicioMes, hoy)).thenReturn(List.of(v));

        byte[] pdf = reporteService.generarReporteVentas(hoy, inicioMes);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        verificaLlamadaVentas(inicioMes, hoy);
    }

    @Test
    void generarReporteVentasSinFechasUsaValoresDefault() {
        // inicio = null → inicioMes(fechaFin); fin = null → HOY
        VentaReporteDTO v = new VentaReporteDTO();
        v.setId(99L);
        v.setFecha(inicioMes);
        v.setMontoTotal(300.0);

        when(ventasClient.obtenerPorRango(inicioMes, hoy)).thenReturn(List.of(v));

        byte[] pdf = reporteService.generarReporteVentas(null, null);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        verificaLlamadaVentas(inicioMes, hoy);
    }

    // ==================== RF-6.2 Reporte de INVENTARIO ====================

    @Test
    void generarReporteInventarioConDatosCriticosYAgotadosRetornaPdf() {
        ProductoReporteDTO pCritico = new ProductoReporteDTO();
        pCritico.setId(1L);
        pCritico.setSku("TEC-001");
        pCritico.setNombre("Notebook");
        pCritico.setSucursal("SUC-CENTRAL");
        pCritico.setStock(2);
        pCritico.setStockMinimo(5);
        pCritico.setEstado("DISPONIBLE");

        ProductoReporteDTO pAgotado = new ProductoReporteDTO();
        pAgotado.setId(2L);
        pAgotado.setSku("HOG-002");
        pAgotado.setNombre("Silla");
        pAgotado.setSucursal("SUC-NORTE");
        pAgotado.setStock(0);
        pAgotado.setStockMinimo(2);
        pAgotado.setEstado("AGOTADO");

        ProductoReporteDTO pDescontinuado = new ProductoReporteDTO();
        pDescontinuado.setId(3L);
        pDescontinuado.setSku("TEC-999");
        pDescontinuado.setNombre("Producto viejo");
        pDescontinuado.setStock(0);
        pDescontinuado.setStockMinimo(10);
        pDescontinuado.setEstado("DESCONTINUADO");  // ← no es crítico por el estado

        when(inventarioClient.obtenerTodos()).thenReturn(List.of(pCritico, pAgotado, pDescontinuado));

        byte[] pdf = reporteService.generarReporteInventario();

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        assertPdfHeader(pdf);
        verify(inventarioClient).obtenerTodos();
    }

    @Test
    void generarReporteInventarioSinProductosRetornaPdfSinExcepcion() {
        when(inventarioClient.obtenerTodos()).thenReturn(List.of());

        byte[] pdf = assertDoesNotThrow(() -> reporteService.generarReporteInventario());

        assertNotNull(pdf);
        assertTrue(pdf.length > 50);
    }

    // ==================== RF-6.2 Reporte de FINANZAS ====================

    @Test
    void generarReporteFinanzasConDatosValidosRetornaPdfConGraficosYTablas() {
        LocalDate inicio = inicioMes;
        LocalDate fin = hoy;

        MovimientoFinancieroReporteDTO m1 = new MovimientoFinancieroReporteDTO();
        m1.setId(1L);
        m1.setFechaRegistro(inicio.plusDays(1));
        m1.setSucursal("SUC-CENTRAL");
        m1.setIngresos(new BigDecimal("5000"));
        m1.setCosto(new BigDecimal("3000"));
        m1.setMargen(new BigDecimal("2000"));

        MovimientoFinancieroReporteDTO m2 = new MovimientoFinancieroReporteDTO();
        m2.setId(2L);
        m2.setFechaRegistro(inicio.plusDays(2));
        m2.setSucursal("SUC-NORTE");
        m2.setIngresos(new BigDecimal("3000"));
        m2.setCosto(new BigDecimal("1500"));
        m2.setMargen(new BigDecimal("1500"));

        ResumenFinancieroDTO resumen = new ResumenFinancieroDTO();
        resumen.setIngresos(new BigDecimal("8000"));
        resumen.setCosto(new BigDecimal("4500"));
        resumen.setMargen(new BigDecimal("3500"));

        when(finanzasClient.obtenerPorRango(inicio, fin)).thenReturn(List.of(m1, m2));
        when(finanzasClient.obtenerTotalesPorRango(inicio, fin)).thenReturn(resumen);

        byte[] pdf = reporteService.generarReporteFinanzas(inicio, fin);

        assertNotNull(pdf);
        assertTrue(pdf.length > 150);
        assertPdfHeader(pdf);
    }

    @Test
    void generarReporteFinanzasConResumenNuloSafeDevuelveCero() {
        // Si el finanzasClient retorna resumen con ingresos, costo, margen null,
        // safe() lo convierte a ZERO (no NPException)
        ResumenFinancieroDTO resumenVacio = new ResumenFinancieroDTO();  // 3 campos null

        when(finanzasClient.obtenerPorRango(inicioMes, hoy)).thenReturn(List.of());
        when(finanzasClient.obtenerTotalesPorRango(inicioMes, hoy)).thenReturn(resumenVacio);

        byte[] pdf = assertDoesNotThrow(() -> reporteService.generarReporteFinanzas(inicioMes, hoy));

        assertNotNull(pdf);
        assertTrue(pdf.length > 50);
    }

    @Test
    void generarReporteFinanzasRangoInvertidoLoCorrigeSilenciosamente() {
        ResumenFinancieroDTO resumen = new ResumenFinancieroDTO();
        resumen.setIngresos(new BigDecimal("100"));
        resumen.setCosto(new BigDecimal("50"));
        resumen.setMargen(new BigDecimal("50"));

        when(finanzasClient.obtenerPorRango(inicioMes, hoy)).thenReturn(List.of());
        when(finanzasClient.obtenerTotalesPorRango(inicioMes, hoy)).thenReturn(resumen);

        byte[] pdf = reporteService.generarReporteFinanzas(hoy, inicioMes);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        verify(finanzasClient).obtenerPorRango(inicioMes, hoy);
        verify(finanzasClient).obtenerTotalesPorRango(inicioMes, hoy);
    }

    // ==================== HALLAZGO: iText cierra streams en crearPdf ====================

    @Test
    void generarReporteVentasConClienteFeignReventadoPropagaRuntimeExceptionDirectamente() {
        //HALLAZGO: la llamada a ventasClient.obtenerPorRango() está FUERA del
        // try-with-resources de `crearPdf()` (línea 65 ≠ línea 70+). Por tanto,
        // si el cliente Feign falla ANTES de empezar a generar el PDF, la
        // excepción NO es envuelta en IllegalStateException — se propaga tal
        // cual (el catch sólo existe dentro de crearPdf).
        when(ventasClient.obtenerPorRango(any(), any()))
                .thenThrow(new RuntimeException("Servicio ventas caído"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reporteService.generarReporteVentas(inicioMes, hoy));

        assertEquals("Servicio ventas caído", ex.getMessage());
    }

    // ==================== Helpers ====================

    private void verificaLlamadaVentas(LocalDate inicio, LocalDate fin) {
        ArgumentCaptor<LocalDate> capInicio = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> capFin = ArgumentCaptor.forClass(LocalDate.class);
        verify(ventasClient).obtenerPorRango(capInicio.capture(), capFin.capture());
        assertEquals(inicio, capInicio.getValue());
        assertEquals(fin, capFin.getValue());
    }

    /**
     * Todos los PDF válidos empiezan con la cabecera mágica "%PDF-1.x"
     * (los 5 primeros bytes son 0x25 0x50 0x44 0x46 0x2D = "%PDF-").
     * Esta es la comprobación más robusta sin parsear el PDF completo.
     */
    private void assertPdfHeader(byte[] pdf) {
        assertNotNull(pdf, "PDF no puede ser null");
        assertTrue(pdf.length >= 5, "PDF demasiado corto: " + pdf.length + " bytes");
        byte[] magic = new byte[]{'%', 'P', 'D', 'F', '-'};
        byte[] primeros = new byte[5];
        System.arraycopy(pdf, 0, primeros, 0, 5);
        assertArrayEquals(magic, primeros,
                "Cabecera PDF inválida. Empieza con: " +
                        new String(primeros).replaceAll("[^\\x20-\\x7E]", "?"));
    }
}
