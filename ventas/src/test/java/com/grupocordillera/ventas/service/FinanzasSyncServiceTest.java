package com.grupocordillera.ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import com.grupocordillera.ventas.client.FinanzasClient;
import com.grupocordillera.ventas.dto.MovimientoFinancieroRequest;
import com.grupocordillera.ventas.model.Venta;
import com.grupocordillera.ventas.repository.VentasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinanzasSyncServiceTest {

    @Mock
    private FinanzasClient finanzasClient;

    @Mock
    private VentasRepository ventasRepository;

    @InjectMocks
    private FinanzasSyncService finanzasSyncService;

    private Venta ventaBase;

    @BeforeEach
    void setUp() {
        ventaBase = new Venta();
        ventaBase.setId(42L);
        ventaBase.setProductoId(7L);
        ventaBase.setSkuProducto("TEC-001");
        ventaBase.setNombreProducto("Notebook");
        ventaBase.setCantidad(2);
        ventaBase.setMontoTotal(1000.0);
        ventaBase.setFecha(LocalDate.now());
        ventaBase.setSucursal("SUC-CENTRAL");
        ventaBase.setClienteId(5L);
    }

    // ==================== RF-5.3: sincronización exitosa ====================

    @Test
    void sincronizarExitosoMarcaEstadoSincronizado() {
        when(ventasRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ventaBase.setEstadoFinanzas(Venta.EstadoFinanzas.PENDIENTE);
        ventaBase.setIntentosFinanzas(2);
        ventaBase.setUltimoErrorFinanzas("Timeout anterior");

        Venta resultado = finanzasSyncService.sincronizarVentaConFinanzas(ventaBase);

        assertEquals(Venta.EstadoFinanzas.SINCRONIZADO, resultado.getEstadoFinanzas());
        assertNull(resultado.getUltimoErrorFinanzas());
        assertNotNull(resultado.getFechaUltimoIntentoFinanzas());
        verify(finanzasClient).registrarMovimiento(any());
        verify(ventasRepository).save(any());
    }

    @Test
    void sincronizarExitosoEnviaRequestConDatosCorrectosDeVenta() {
        when(ventasRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<MovimientoFinancieroRequest> captor =
                ArgumentCaptor.forClass(MovimientoFinancieroRequest.class);

        finanzasSyncService.sincronizarVentaConFinanzas(ventaBase);

        verify(finanzasClient).registrarMovimiento(captor.capture());
        MovimientoFinancieroRequest enviado = captor.getValue();
        assertEquals(42L, enviado.getVentaId());
        assertEquals(7L, enviado.getProductoId());
        assertEquals("TEC-001", enviado.getSkuProducto());
        assertEquals("Notebook", enviado.getNombreProducto());
        assertEquals(2, enviado.getCantidad());
        assertEquals(MovimientoFinancieroRequest.TipoMovimiento.INGRESO, enviado.getTipoMovimiento());
        assertEquals("SUC-CENTRAL", enviado.getSucursal());
    }

    // ==================== RF-5.3: fallback (Circuit Breaker abierto / excepción) ====================

    @Test
    void fallbackPrimerIntentoMarcaPendienteEIncrementaContador() {
        when(ventasRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ventaBase.setIntentosFinanzas(null);
        ventaBase.setEstadoFinanzas(Venta.EstadoFinanzas.PENDIENTE);

        Venta resultado = finanzasSyncService.fallbackSincronizarFinanzas(ventaBase,
                new RuntimeException("Finanzas caído"));

        assertEquals(Integer.valueOf(1), resultado.getIntentosFinanzas());
        assertEquals(Venta.EstadoFinanzas.PENDIENTE, resultado.getEstadoFinanzas());
        assertEquals("Finanzas caído", resultado.getUltimoErrorFinanzas());
        assertNotNull(resultado.getFechaUltimoIntentoFinanzas());
        verify(ventasRepository).save(any());
    }

    @Test
    void fallbackSegundoIntentoSigueSiendoPendiente() {
        when(ventasRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ventaBase.setIntentosFinanzas(1);
        ventaBase.setEstadoFinanzas(Venta.EstadoFinanzas.PENDIENTE);

        Venta resultado = finanzasSyncService.fallbackSincronizarFinanzas(ventaBase,
                new RuntimeException("Falló de nuevo"));

        assertEquals(Integer.valueOf(2), resultado.getIntentosFinanzas());
        assertEquals(Venta.EstadoFinanzas.PENDIENTE, resultado.getEstadoFinanzas());
    }

    @Test
    void fallbackTrasCincoIntentosPasaAEstadoError() {
        // MAX_INTENTOS = 5. Con 4 actuales, luego del incremento llega a 5 → ERROR.
        when(ventasRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ventaBase.setIntentosFinanzas(4);
        ventaBase.setEstadoFinanzas(Venta.EstadoFinanzas.PENDIENTE);

        Venta resultado = finanzasSyncService.fallbackSincronizarFinanzas(ventaBase,
                new RuntimeException("Intento final fallido"));

        assertEquals(Integer.valueOf(5), resultado.getIntentosFinanzas());
        assertEquals(Venta.EstadoFinanzas.ERROR, resultado.getEstadoFinanzas());
    }

    @Test
    void fallbackIntentoNumeroSeisSigueEnError() {
        when(ventasRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ventaBase.setIntentosFinanzas(5);
        ventaBase.setEstadoFinanzas(Venta.EstadoFinanzas.ERROR);

        Venta resultado = finanzasSyncService.fallbackSincronizarFinanzas(ventaBase,
                new RuntimeException("reintento espurio"));

        assertEquals(Integer.valueOf(6), resultado.getIntentosFinanzas());
        assertEquals(Venta.EstadoFinanzas.ERROR, resultado.getEstadoFinanzas());
    }

    @Test
    void fallbackExtrayeMensajeDeCausaRaiz() {
        when(ventasRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ventaBase.setIntentosFinanzas(0);

        RuntimeException causaRaiz = new RuntimeException("Causa final");
        RuntimeException intermedia = new RuntimeException("Intermedia", causaRaiz);
        RuntimeException exterior = new RuntimeException("Exterior", intermedia);

        Venta resultado = finanzasSyncService.fallbackSincronizarFinanzas(ventaBase, exterior);

        assertEquals("Causa final", resultado.getUltimoErrorFinanzas());
    }

    @Test
    void fallbackSiTodasLasCausasSonNulasDevuelveSinDetalle() {
        when(ventasRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ventaBase.setIntentosFinanzas(0);

        Venta resultado = finanzasSyncService.fallbackSincronizarFinanzas(ventaBase,
                new RuntimeException((String) null));

        assertEquals("sin detalle", resultado.getUltimoErrorFinanzas());
    }

    @Test
    void fallbackNoLlamaAClienteFinanzas() {
        when(ventasRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ventaBase.setIntentosFinanzas(1);

        finanzasSyncService.fallbackSincronizarFinanzas(ventaBase, new RuntimeException("fallo"));

        verify(finanzasClient, never()).registrarMovimiento(any());
    }
}
