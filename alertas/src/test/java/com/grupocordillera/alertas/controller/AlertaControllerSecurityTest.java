package com.grupocordillera.alertas.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.grupocordillera.alertas.service.AlertaService;
import com.grupocordillera.alertas.service.DeteccionAlertasService;

class AlertaControllerSecurityTest {
    @Test
    void endpointDeDiagnosticoNoEstaExpuesto() throws Exception {
        var controller = new AlertaController(mock(AlertaService.class), mock(DeteccionAlertasService.class));
        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(get("/alertas/test-inventario"))
                .andExpect(status().isNotFound());
    }
}
