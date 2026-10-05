package com.grupocordillera.alertas.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.grupocordillera.alertas.dto.AlertaResponse;
import com.grupocordillera.alertas.dto.AlertasResumenResponse;
import com.grupocordillera.alertas.dto.CrearAlertaRequest;
import com.grupocordillera.alertas.model.Alerta;
import com.grupocordillera.alertas.repository.AlertaRepository;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock
    private AlertaRepository alertaRepository;

    @InjectMocks
    private AlertaService alertaService;

    private Alerta alertaStockCritico;
    private Alerta alertaVentasBajas;
    private Alerta alertaMargenLeida;

    @BeforeEach
    void setUp() {
        LocalDateTime ahora = LocalDateTime.now();

        alertaStockCritico = new Alerta();
        alertaStockCritico.setId(1L);
        alertaStockCritico.setCodigo("STOCK_CRITICO:TEC-001");
        alertaStockCritico.setTipo(Alerta.TipoAlerta.STOCK_CRITICO);
        alertaStockCritico.setTitulo("Stock crítico");
        alertaStockCritico.setDetalle("Notebook");
        alertaStockCritico.setLeida(false);
        alertaStockCritico.setFechaCreacion(ahora.minusMinutes(5));
        alertaStockCritico.setFechaLectura(null);

        alertaVentasBajas = new Alerta();
        alertaVentasBajas.setId(2L);
        alertaVentasBajas.setCodigo("VENTAS_BAJAS:SUC-CENTRAL");
        alertaVentasBajas.setTipo(Alerta.TipoAlerta.VENTAS_BAJAS);
        alertaVentasBajas.setTitulo("Ventas bajas");
        alertaVentasBajas.setDetalle("Sucursal SUC-CENTRAL");
        alertaVentasBajas.setLeida(false);
        alertaVentasBajas.setFechaCreacion(ahora.minusMinutes(45));

        alertaMargenLeida = new Alerta();
        alertaMargenLeida.setId(3L);
        alertaMargenLeida.setCodigo("MARGEN_NORMALIZADO:FINANZAS");
        alertaMargenLeida.setTipo(Alerta.TipoAlerta.MARGEN_NORMALIZADO);
        alertaMargenLeida.setTitulo("Margen normalizado");
        alertaMargenLeida.setDetalle("Finanzas");
        alertaMargenLeida.setLeida(true);
        alertaMargenLeida.setFechaCreacion(ahora.minusHours(2));
        alertaMargenLeida.setFechaLectura(ahora.minusHours(1));
    }

    // ==================== RF-6.2: crearAlerta (resumen y detalle) ====================

    @Test
    void crearAlertaNuevaSinCodigoGeneraCodigoManualYGuarda() {
        CrearAlertaRequest request = new CrearAlertaRequest();
        request.setTipo(Alerta.TipoAlerta.INFORMACION);
        request.setCodigo(null);
        request.setTitulo("  Prueba manual  ");
        request.setDetalle("  Detalle de prueba  ");

        when(alertaRepository.findByCodigo("MANUAL:INFORMACION:Prueba manual"))
                .thenReturn(Optional.empty());
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> {
            Alerta a = invocation.getArgument(0);
            a.setId(10L);
            return a;
        });

        AlertaResponse resultado = alertaService.crearAlerta(request);

        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepository).save(captor.capture());
        Alerta guardada = captor.getValue();

        assertEquals(10L, resultado.getId());
        assertEquals("MANUAL:INFORMACION:Prueba manual", guardada.getCodigo());
        assertEquals("Prueba manual", resultado.getTitulo());
        assertEquals("Detalle de prueba", resultado.getDetalle());
        assertFalse(resultado.getLeida());
        assertNull(resultado.getFechaLectura());
        assertNotNull(resultado.getFechaCreacion());
        assertEquals("ℹ️", resultado.getIcono());
        assertEquals("Prueba manual - Detalle de prueba", resultado.getTituloCompleto());
    }

    @Test
    void crearAlertaConCodigoExplicitoLoUsaYNoGeneraManual() {
        CrearAlertaRequest request = new CrearAlertaRequest();
        request.setTipo(Alerta.TipoAlerta.INFORMACION);
        request.setCodigo("  MI-CODIGO-123  ");
        request.setTitulo("Alerta custom");
        request.setDetalle("Detalle custom");

        when(alertaRepository.findByCodigo("MI-CODIGO-123")).thenReturn(Optional.empty());
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        alertaService.crearAlerta(request);

        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepository).save(captor.capture());
        assertEquals("MI-CODIGO-123", captor.getValue().getCodigo());
    }

    @Test
    void crearAlertaConCodigoDuplicadoActualizaLaExistenteYDesmarcaLeida() {
        Alerta existente = new Alerta();
        existente.setId(5L);
        existente.setCodigo("MANUAL:INFORMACION:Info antigua");
        existente.setTipo(Alerta.TipoAlerta.INFORMACION);
        existente.setTitulo("Info antigua");
        existente.setDetalle("Detalle antiguo");
        existente.setLeida(true);
        existente.setFechaCreacion(LocalDateTime.now().minusDays(1));
        existente.setFechaLectura(LocalDateTime.now().minusHours(5));

        when(alertaRepository.findByCodigo("MANUAL:INFORMACION:Info antigua"))
                .thenReturn(Optional.of(existente));
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CrearAlertaRequest request = new CrearAlertaRequest();
        request.setTipo(Alerta.TipoAlerta.INFORMACION);
        request.setTitulo("Info antigua");
        request.setDetalle("Detalle NUEVO");

        AlertaResponse resultado = alertaService.crearAlerta(request);

        assertEquals(5L, resultado.getId());
        assertEquals("Detalle NUEVO", resultado.getDetalle());
        // Vuelve a marcar como NO leída
        assertFalse(resultado.getLeida());
        assertNull(resultado.getFechaLectura());
        // FechaCreacion se MANTIENE (no se sobrescribe)
        assertNotNull(resultado.getFechaCreacion());
    }

    // ==================== RF-6.2: obtenerTodas / obtenerNoLeidas / contar / resumen ====================

    @Test
    void obtenerTodasRetornaTodasOrdenadasPorFechaDesc() {
        when(alertaRepository.findAllByOrderByFechaCreacionDesc())
                .thenReturn(List.of(alertaStockCritico, alertaVentasBajas, alertaMargenLeida));

        List<AlertaResponse> resultado = alertaService.obtenerTodas();

        assertEquals(3, resultado.size());
        assertEquals("Stock crítico", resultado.get(0).getTitulo());
        assertEquals("Ventas bajas", resultado.get(1).getTitulo());
        assertEquals("⚠️", resultado.get(0).getIcono());
        assertEquals("⚠️", resultado.get(1).getIcono());
        assertEquals("✅", resultado.get(2).getIcono());
    }

    @Test
    void obtenerNoLeidasFiltraSoloLasPendientes() {
        when(alertaRepository.findByLeidaFalseOrderByFechaCreacionDesc())
                .thenReturn(List.of(alertaStockCritico, alertaVentasBajas));

        List<AlertaResponse> resultado = alertaService.obtenerNoLeidas();

        assertEquals(2, resultado.size());
        assertFalse(resultado.get(0).getLeida());
        assertFalse(resultado.get(1).getLeida());
    }

    @Test
    void contarNoLeidasDevuelveElValorDelRepository() {
        when(alertaRepository.countByLeidaFalse()).thenReturn(2L);

        long resultado = alertaService.contarNoLeidas();

        assertEquals(2L, resultado);
    }

    @Test
    void obtenerResumenCombinaConteoYListaNoLeidas() {
        when(alertaRepository.countByLeidaFalse()).thenReturn(2L);
        when(alertaRepository.findByLeidaFalseOrderByFechaCreacionDesc())
                .thenReturn(List.of(alertaStockCritico, alertaVentasBajas));

        AlertasResumenResponse resumen = alertaService.obtenerResumen();

        assertEquals(2L, resumen.getNoLeidas());
        assertEquals(2, resumen.getAlertas().size());
        assertEquals("Stock crítico - Notebook", resumen.getAlertas().get(0).getTituloCompleto());
        assertEquals("Ventas bajas - Sucursal SUC-CENTRAL",
                resumen.getAlertas().get(1).getTituloCompleto());
    }

    // ==================== RF-6.2: marcarComoLeida / marcarTodasComoLeidas ====================

    @Test
    void marcarComoLeidaCambiaEstadoYFechaYGuarda() {
        when(alertaRepository.findById(1L)).thenReturn(Optional.of(alertaStockCritico));
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertaResponse resultado = alertaService.marcarComoLeida(1L);

        assertTrue(resultado.getLeida());
        assertNotNull(resultado.getFechaLectura());
        verify(alertaRepository).save(alertaStockCritico);
    }

    @Test
    void marcarComoLeidaSiYaEstabaLeidaNoVuelveAGuardar() {
        when(alertaRepository.findById(3L)).thenReturn(Optional.of(alertaMargenLeida));

        AlertaResponse resultado = alertaService.marcarComoLeida(3L);

        assertTrue(resultado.getLeida());
        assertNotNull(resultado.getFechaLectura());
        verify(alertaRepository, never()).save(any());
    }

    @Test
    void marcarComoLeidaConIdInexistenteLanzaError() {
        when(alertaRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> alertaService.marcarComoLeida(999L));

        assertEquals("Alerta no encontrada con id: 999", ex.getMessage());
    }

    @Test
    void marcarTodasComoLeidasMarcaTodasYPersisteViaSaveAll() {
        when(alertaRepository.findByLeidaFalseOrderByFechaCreacionDesc())
                .thenReturn(List.of(alertaStockCritico, alertaVentasBajas));
        ArgumentCaptor<List<Alerta>> captor = ArgumentCaptor.forClass(List.class);
        when(alertaRepository.saveAll(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        List<AlertaResponse> resultado = alertaService.marcarTodasComoLeidas();

        assertEquals(2, resultado.size());
        List<Alerta> guardadas = captor.getValue();
        assertTrue(guardadas.get(0).getLeida());
        assertTrue(guardadas.get(1).getLeida());
        assertNotNull(guardadas.get(0).getFechaLectura());
        assertNotNull(guardadas.get(1).getFechaLectura());
    }

    @Test
    void marcarTodasComoLeidasConListaVaciaDevuelveVacio() {
        when(alertaRepository.findByLeidaFalseOrderByFechaCreacionDesc())
                .thenReturn(List.of());

        List<AlertaResponse> resultado = alertaService.marcarTodasComoLeidas();

        assertEquals(0, resultado.size());
        // saveAll SÍ se llama con lista vacía por el service, es el comportamiento actual
    }

    // ==================== toResponse: iconos por tipo ====================

    @Test
    void iconoPorTipoDevuelveElEsperado() {
        // MARGEN_NORMALIZADO → ✅
        when(alertaRepository.findById(3L)).thenReturn(Optional.of(alertaMargenLeida));
        AlertaResponse rMargen = alertaService.marcarComoLeida(3L);
        assertEquals("✅", rMargen.getIcono());

        // INFORMACION → ℹ️
        Alerta info = new Alerta();
        info.setId(4L);
        info.setTipo(Alerta.TipoAlerta.INFORMACION);
        info.setTitulo("Info");
        info.setDetalle("D");
        info.setCodigo("INFO:1");
        info.setFechaCreacion(LocalDateTime.now());
        info.setLeida(false);
        when(alertaRepository.findById(4L)).thenReturn(Optional.of(info));
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AlertaResponse rInfo = alertaService.marcarComoLeida(4L);
        assertEquals("ℹ️", rInfo.getIcono());

        // STOCK_CRITICO → ⚠️
        when(alertaRepository.findById(1L)).thenReturn(Optional.of(alertaStockCritico));
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AlertaResponse rStock = alertaService.marcarComoLeida(1L);
        assertEquals("⚠️", rStock.getIcono());
    }

    // ==================== toResponse: tiempo relativo ====================

    @Test
    void tiempoRelativoMuestraHaceUnMomentoSiRecienCreada() {
        Alerta recien = new Alerta();
        recien.setId(5L);
        recien.setTipo(Alerta.TipoAlerta.INFORMACION);
        recien.setTitulo("T");
        recien.setDetalle("D");
        recien.setCodigo("X:1");
        recien.setFechaCreacion(LocalDateTime.now().minusSeconds(30));
        when(alertaRepository.findByCodigo("X:1")).thenReturn(Optional.empty());
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CrearAlertaRequest req = new CrearAlertaRequest();
        req.setTipo(Alerta.TipoAlerta.INFORMACION);
        req.setCodigo("X:1");
        req.setTitulo("T");
        req.setDetalle("D");

        AlertaResponse resultado = alertaService.crearAlerta(req);

        assertEquals("hace un momento", resultado.getTiempoRelativo());
    }

    @Test
    void tiempoRelativoMuestraMinutos() {
        when(alertaRepository.findById(2L)).thenReturn(Optional.of(alertaVentasBajas));
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertaResponse r = alertaService.marcarComoLeida(2L);

        assertTrue(r.getTiempoRelativo().startsWith("hace "));
        assertTrue(r.getTiempoRelativo().contains("min"));
    }
}
