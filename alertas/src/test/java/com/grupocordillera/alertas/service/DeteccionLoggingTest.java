package com.grupocordillera.alertas.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.grupocordillera.alertas.client.FinanzasClient;
import com.grupocordillera.alertas.client.InventarioClient;
import com.grupocordillera.alertas.client.VentasClient;
import com.grupocordillera.alertas.repository.AlertaRepository;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class DeteccionLoggingTest {
    @Test
    void fallaExternaNoRegistraSuPayloadNiStackTrace() {
        var inventario = mock(InventarioClient.class);
        var ventas = mock(VentasClient.class);
        var finanzas = mock(FinanzasClient.class);
        var service = new DeteccionAlertasService(mock(AlertaRepository.class), inventario, ventas, finanzas);
        String sensitiveMessage = "Authorization: Bearer dato-que-no-debe-registrarse";
        when(inventario.obtenerProductosRaw()).thenThrow(new RuntimeException(sensitiveMessage));
        when(ventas.obtenerVentas()).thenReturn(List.of());
        when(finanzas.obtenerMovimientos()).thenReturn(List.of());
        Logger logger = (Logger) LoggerFactory.getLogger(DeteccionAlertasService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            service.detectarAhora();

            assertFalse(appender.list.isEmpty());
            for (ILoggingEvent event : appender.list) {
                assertFalse(event.getFormattedMessage().contains(sensitiveMessage));
                assertNull(event.getThrowableProxy());
            }
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
