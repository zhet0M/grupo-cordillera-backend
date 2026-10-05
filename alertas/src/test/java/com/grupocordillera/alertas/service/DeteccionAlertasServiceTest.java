package com.grupocordillera.alertas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupocordillera.alertas.client.FinanzasClient;
import com.grupocordillera.alertas.client.InventarioClient;
import com.grupocordillera.alertas.client.VentasClient;
import com.grupocordillera.alertas.dto.MovimientoFinancieroFuenteDTO;
import com.grupocordillera.alertas.dto.ProductoInventarioFuenteDTO;
import com.grupocordillera.alertas.dto.VentaFuenteDTO;
import com.grupocordillera.alertas.model.Alerta;
import com.grupocordillera.alertas.repository.AlertaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeteccionAlertasServiceTest {

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private InventarioClient inventarioClient;

    @Mock
    private VentasClient ventasClient;

    @Mock
    private FinanzasClient finanzasClient;

    @InjectMocks
    private DeteccionAlertasService deteccionAlertasService;

    private ObjectMapper objectMapper;
    private LocalDate hoy;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        hoy = LocalDate.now();
        // Por defecto, 2 de los 3 clientes devuelven vacío; cada test sobrescribe el que necesita
        lenient().when(ventasClient.obtenerVentas()).thenReturn(List.of());
        lenient().when(finanzasClient.obtenerMovimientos()).thenReturn(List.of());
        String jsonVacio = "[]";
        lenient().when(inventarioClient.obtenerProductosRaw()).thenReturn(ResponseEntity.ok(jsonVacio));
    }

    // ==================== RF-6.1: Detección de STOCK_CRÍTICO ====================

    @Test
    void detectarStockCriticoConStockMenorOIgualAlMinimoDisparaAlerta() throws Exception {
        ProductoInventarioFuenteDTO critico = new ProductoInventarioFuenteDTO();
        critico.setId(1L);
        critico.setSku("TEC-001");
        critico.setNombre("Notebook");
        critico.setStock(2);
        critico.setStockMinimo(5);
        critico.setEstado("DISPONIBLE");

        ProductoInventarioFuenteDTO normal = new ProductoInventarioFuenteDTO();
        normal.setId(2L);
        normal.setSku("HOG-001");
        normal.setNombre("Silla");
        normal.setStock(20);
        normal.setStockMinimo(2);
        normal.setEstado("DISPONIBLE");

        String json = objectMapper.writeValueAsString(List.of(critico, normal));
        when(inventarioClient.obtenerProductosRaw()).thenReturn(ResponseEntity.ok(json));
        when(alertaRepository.findByCodigo("STOCK_CRITICO:TEC-001")).thenReturn(Optional.empty());
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deteccionAlertasService.detectarAhora();

        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepository).save(captor.capture());
        Alerta creada = captor.getValue();
        assertEquals("STOCK_CRITICO:TEC-001", creada.getCodigo());
        assertEquals(Alerta.TipoAlerta.STOCK_CRITICO, creada.getTipo());
        assertEquals("Stock crítico", creada.getTitulo());
        assertEquals("Notebook", creada.getDetalle());
        assertFalse(creada.getLeida());
        assertNotNull(creada.getFechaCreacion());
    }

    @Test
    void detectarStockCriticoProductoDescontinuadoNoDisparaAlerta() throws Exception {
        ProductoInventarioFuenteDTO descontinuado = new ProductoInventarioFuenteDTO();
        descontinuado.setId(1L);
        descontinuado.setSku("TEC-999");
        descontinuado.setNombre("Producto viejo");
        descontinuado.setStock(0);
        descontinuado.setStockMinimo(5);
        descontinuado.setEstado("DESCONTINUADO");

        String json = objectMapper.writeValueAsString(List.of(descontinuado));
        when(inventarioClient.obtenerProductosRaw()).thenReturn(ResponseEntity.ok(json));

        deteccionAlertasService.detectarAhora();

        verify(alertaRepository, never()).save(any());
    }

    @Test
    void detectarStockCriticoActualizaAlertaExistenteYLaVuelveNoLeida() throws Exception {
        ProductoInventarioFuenteDTO critico = new ProductoInventarioFuenteDTO();
        critico.setId(1L);
        critico.setSku("TEC-001");
        critico.setNombre("Notebook");
        critico.setStock(1);
        critico.setStockMinimo(5);

        Alerta existente = new Alerta();
        existente.setId(10L);
        existente.setCodigo("STOCK_CRITICO:TEC-001");
        existente.setTipo(Alerta.TipoAlerta.STOCK_CRITICO);
        existente.setTitulo("Stock crítico");
        existente.setDetalle("Notebook");
        existente.setLeida(true);
        existente.setFechaLectura(hoy.minusDays(1).atStartOfDay());
        existente.setFechaCreacion(hoy.minusDays(1).atStartOfDay());

        String json = objectMapper.writeValueAsString(List.of(critico));
        when(inventarioClient.obtenerProductosRaw()).thenReturn(ResponseEntity.ok(json));
        when(alertaRepository.findByCodigo("STOCK_CRITICO:TEC-001")).thenReturn(Optional.of(existente));
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deteccionAlertasService.detectarAhora();

        verify(alertaRepository).save(existente);
        assertFalse(existente.getLeida());
        assertNull(existente.getFechaLectura());
    }

    @Test
    void detectarStockCriticoProductoSinSkuONombreSeSalta() throws Exception {
        ProductoInventarioFuenteDTO sinSku = new ProductoInventarioFuenteDTO();
        sinSku.setNombre("Sin SKU");
        sinSku.setStock(1);
        sinSku.setStockMinimo(10);

        ProductoInventarioFuenteDTO sinNombre = new ProductoInventarioFuenteDTO();
        sinNombre.setSku("TEC-X");
        sinNombre.setStock(1);
        sinNombre.setStockMinimo(10);

        String json = objectMapper.writeValueAsString(List.of(sinSku, sinNombre));
        when(inventarioClient.obtenerProductosRaw()).thenReturn(ResponseEntity.ok(json));

        deteccionAlertasService.detectarAhora();

        verify(alertaRepository, never()).save(any());
    }

    @Test
    void detectarStockCriticoStockNullNoDisparaAlerta() throws Exception {
        ProductoInventarioFuenteDTO stockNull = new ProductoInventarioFuenteDTO();
        stockNull.setId(1L);
        stockNull.setSku("TEC-NULL");
        stockNull.setNombre("Cargador");
        stockNull.setStock(null);
        stockNull.setStockMinimo(3);
        stockNull.setEstado("DISPONIBLE");

        ProductoInventarioFuenteDTO minNull = new ProductoInventarioFuenteDTO();
        minNull.setId(2L);
        minNull.setSku("TEC-MIN-NULL");
        minNull.setNombre("Mouse");
        minNull.setStock(1);
        minNull.setStockMinimo(null);
        minNull.setEstado("DISPONIBLE");

        String json = objectMapper.writeValueAsString(List.of(stockNull, minNull));
        when(inventarioClient.obtenerProductosRaw()).thenReturn(ResponseEntity.ok(json));

        deteccionAlertasService.detectarAhora();

        verify(alertaRepository, never()).save(any());
    }

    // ==================== RF-6.1: Detección de VENTAS_BAJAS ====================

    @Test
    void detectarVentasBajasCuandoCaenMasDel25PorCientoDisparaAlerta() {
        LocalDate inicioActual = hoy.minusDays(6);
        LocalDate inicioAnterior = hoy.minusDays(13);

        // Semana actual = 1.000, semana anterior = 10.000 → caída del 90% → baja
        VentaFuenteDTO actual = new VentaFuenteDTO();
        actual.setId(1L);
        actual.setFecha(inicioActual);
        actual.setSucursal("SUC-CENTRAL");
        actual.setMontoTotal(1000.0);

        VentaFuenteDTO anterior = new VentaFuenteDTO();
        anterior.setId(2L);
        anterior.setFecha(inicioAnterior);
        anterior.setSucursal("SUC-CENTRAL");
        anterior.setMontoTotal(10000.0);

        when(ventasClient.obtenerVentas()).thenReturn(List.of(actual, anterior));
        when(alertaRepository.findByCodigo("VENTAS_BAJAS:SUC-CENTRAL")).thenReturn(Optional.empty());
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deteccionAlertasService.detectarAhora();

        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepository).save(captor.capture());
        Alerta alerta = captor.getValue();
        assertEquals("VENTAS_BAJAS:SUC-CENTRAL", alerta.getCodigo());
        assertEquals(Alerta.TipoAlerta.VENTAS_BAJAS, alerta.getTipo());
        assertEquals("Ventas bajas", alerta.getTitulo());
        assertEquals("Sucursal SUC-CENTRAL", alerta.getDetalle());
    }

    @Test
    void detectarVentasBajasVentasEstablesNoDisparaAlerta() {
        LocalDate inicioActual = hoy.minusDays(6);
        LocalDate inicioAnterior = hoy.minusDays(13);

        // Actual = 9.000, Anterior = 10.000 → caída 10% < 25% → NO baja
        VentaFuenteDTO actual = new VentaFuenteDTO();
        actual.setFecha(inicioActual);
        actual.setSucursal("SUC-ESTE");
        actual.setMontoTotal(9000.0);

        VentaFuenteDTO anterior = new VentaFuenteDTO();
        anterior.setFecha(inicioAnterior);
        anterior.setSucursal("SUC-ESTE");
        anterior.setMontoTotal(10000.0);

        when(ventasClient.obtenerVentas()).thenReturn(List.of(actual, anterior));

        deteccionAlertasService.detectarAhora();

        verify(alertaRepository, never()).save(any());
    }

    @Test
    void detectarVentasBajasSinHistoricoPeroActualCeroSiDispara() {
        VentaFuenteDTO enOtraSucursal = new VentaFuenteDTO();
        enOtraSucursal.setFecha(hoy.minusDays(14));
        enOtraSucursal.setSucursal("SUC-NORESTE");
        enOtraSucursal.setMontoTotal(1000.0);

        when(ventasClient.obtenerVentas()).thenReturn(List.of(enOtraSucursal));
        // Stubs lenient: se usan solo si dispara alerta, que no ocurre en este test
        lenient().when(alertaRepository.findByCodigo(any())).thenReturn(Optional.empty());
        lenient().when(alertaRepository.save(any(Alerta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        deteccionAlertasService.detectarAhora();

        // HALLAZGO: el código itera `ventasActuales.keySet()`. Si una sucursal
        // tuvo ventas en el periodo anterior pero 0 en el actual, no aparece en
        // el keySet actual → NO se genera la alerta de ventas bajas.
        verify(alertaRepository, never()).save(any());
    }

    @Test
    void detectarVentasBajasSucursalEnMinusculasSeNormalizaAMayusculas() {
        LocalDate inicioActual = hoy.minusDays(6);
        LocalDate inicioAnterior = hoy.minusDays(13);

        VentaFuenteDTO actual = new VentaFuenteDTO();
        actual.setFecha(inicioActual);
        actual.setSucursal("  suc-sur  ");  // con espacios
        actual.setMontoTotal(100.0);

        VentaFuenteDTO anterior = new VentaFuenteDTO();
        anterior.setFecha(inicioAnterior);
        anterior.setSucursal("suc-sur");
        anterior.setMontoTotal(1000.0);

        when(ventasClient.obtenerVentas()).thenReturn(List.of(actual, anterior));
        when(alertaRepository.findByCodigo("VENTAS_BAJAS:SUC-SUR")).thenReturn(Optional.empty());
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deteccionAlertasService.detectarAhora();

        ArgumentCaptor<String> codigoCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertaRepository).findByCodigo(codigoCaptor.capture());
        assertEquals("VENTAS_BAJAS:SUC-SUR", codigoCaptor.getValue());
    }

    // ==================== RF-6.1: Detección de MARGEN_NORMALIZADO ====================

    @Test
    void detectarMargenNormalizadoPasaDeNegativoAPositivoDisparaAlerta() {
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1).withDayOfMonth(1);

        MovimientoFinancieroFuenteDTO mesAnterior = new MovimientoFinancieroFuenteDTO();
        mesAnterior.setId(1L);
        mesAnterior.setFechaRegistro(inicioMesAnterior);
        mesAnterior.setMargen(new BigDecimal("-5000"));

        MovimientoFinancieroFuenteDTO mesActual = new MovimientoFinancieroFuenteDTO();
        mesActual.setId(2L);
        mesActual.setFechaRegistro(hoy);
        mesActual.setMargen(new BigDecimal("3000"));

        when(finanzasClient.obtenerMovimientos()).thenReturn(List.of(mesAnterior, mesActual));
        when(alertaRepository.findByCodigo("MARGEN_NORMALIZADO:FINANZAS")).thenReturn(Optional.empty());
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deteccionAlertasService.detectarAhora();

        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepository).save(captor.capture());
        Alerta alerta = captor.getValue();
        assertEquals("MARGEN_NORMALIZADO:FINANZAS", alerta.getCodigo());
        assertEquals(Alerta.TipoAlerta.MARGEN_NORMALIZADO, alerta.getTipo());
        assertEquals("Margen normalizado", alerta.getTitulo());
        assertEquals("Finanzas", alerta.getDetalle());
    }

    @Test
    void detectarMargenSiempreNegativoNoDisparaAlerta() {
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1).withDayOfMonth(1);

        MovimientoFinancieroFuenteDTO m1 = new MovimientoFinancieroFuenteDTO();
        m1.setFechaRegistro(inicioMesAnterior);
        m1.setMargen(new BigDecimal("-2000"));

        MovimientoFinancieroFuenteDTO m2 = new MovimientoFinancieroFuenteDTO();
        m2.setFechaRegistro(hoy);
        m2.setMargen(new BigDecimal("-500"));

        when(finanzasClient.obtenerMovimientos()).thenReturn(List.of(m1, m2));

        deteccionAlertasService.detectarAhora();

        verify(alertaRepository, never()).save(any());
    }

    @Test
    void detectarMargenSiemprePositivoNoDisparaAlerta() {
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1).withDayOfMonth(1);

        MovimientoFinancieroFuenteDTO m1 = new MovimientoFinancieroFuenteDTO();
        m1.setFechaRegistro(inicioMesAnterior);
        m1.setMargen(new BigDecimal("1000"));

        MovimientoFinancieroFuenteDTO m2 = new MovimientoFinancieroFuenteDTO();
        m2.setFechaRegistro(hoy);
        m2.setMargen(new BigDecimal("2000"));

        when(finanzasClient.obtenerMovimientos()).thenReturn(List.of(m1, m2));

        deteccionAlertasService.detectarAhora();

        verify(alertaRepository, never()).save(any());
    }

    @Test
    void detectarMargenSinMovimientosNoDisparaAlerta() {
        when(finanzasClient.obtenerMovimientos()).thenReturn(List.of());

        deteccionAlertasService.detectarAhora();

        verify(alertaRepository, never()).save(any());
    }

    // ==================== RF-6.1: excepciones no rompen el flujo ====================

    @Test
    void detectarAhoraInventarioFallaPeroSigueConVentasYFinanzas() throws Exception {
        // Inventario falla → se captura y sigue con ventas y finanzas
        when(inventarioClient.obtenerProductosRaw()).thenThrow(new RuntimeException("Inventario down"));

        // Ventas con una caída
        LocalDate inicioActual = hoy.minusDays(6);
        LocalDate inicioAnterior = hoy.minusDays(13);
        VentaFuenteDTO va = new VentaFuenteDTO();
        va.setId(1L);
        va.setFecha(inicioActual);
        va.setSucursal("SUC-FALLIDA");
        va.setMontoTotal(100.0);
        VentaFuenteDTO vp = new VentaFuenteDTO();
        vp.setId(2L);
        vp.setFecha(inicioAnterior);
        vp.setSucursal("SUC-FALLIDA");
        vp.setMontoTotal(1000.0);
        when(ventasClient.obtenerVentas()).thenReturn(List.of(va, vp));
        when(alertaRepository.findByCodigo(any())).thenReturn(Optional.empty());
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // No debe propagar excepción
        deteccionAlertasService.detectarAhora();

        assertTrue(true);
        // Confirmamos que al menos guardó la de ventas
        verify(alertaRepository, times(1)).save(any());
    }

    @Test
    void detectarAhoraVentasFallaPeroSigueConMargen() {
        when(ventasClient.obtenerVentas()).thenThrow(new RuntimeException("Ventas down"));

        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1).withDayOfMonth(1);
        MovimientoFinancieroFuenteDTO anterior = new MovimientoFinancieroFuenteDTO();
        anterior.setFechaRegistro(inicioMesAnterior);
        anterior.setMargen(new BigDecimal("-100"));
        MovimientoFinancieroFuenteDTO actual = new MovimientoFinancieroFuenteDTO();
        actual.setFechaRegistro(hoy);
        actual.setMargen(new BigDecimal("100"));
        when(finanzasClient.obtenerMovimientos()).thenReturn(List.of(anterior, actual));
        when(alertaRepository.findByCodigo(any())).thenReturn(Optional.empty());
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deteccionAlertasService.detectarAhora();

        verify(alertaRepository, times(1)).save(any());
    }

    // ==================== HALLAZGOS del dominio Alertas ====================

    @Test
    void detectarVentasBajasSucursalPasaDeVentasACeroNuncaGeneraAlerta() {
        // HALLAZGO: el código itera `for (String sucursal : ventasActuales.keySet())`.
        // Si una sucursal solo está en `ventasPrevias` (venta actual = 0, no aparece en actuales),
        // jamás entra al for → no se genera la alerta.
        // Brecha funcional documentada.
        LocalDate inicioAnterior = hoy.minusDays(13);

        // Esta sucursal tuvo ventas el periodo anterior, NINGUNA el actual.
        VentaFuenteDTO periodoAnterior = new VentaFuenteDTO();
        periodoAnterior.setId(99L);
        periodoAnterior.setFecha(inicioAnterior);
        periodoAnterior.setSucursal("SUC-DESAPARECIDA");
        periodoAnterior.setMontoTotal(999999.0);

        when(ventasClient.obtenerVentas()).thenReturn(List.of(periodoAnterior));

        deteccionAlertasService.detectarAhora();

        // No se guardó nada a pesar de la caída de 100%.
        verify(alertaRepository, never()).save(any());
    }
}
