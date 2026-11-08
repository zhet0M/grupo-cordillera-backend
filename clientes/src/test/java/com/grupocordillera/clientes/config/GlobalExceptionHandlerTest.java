package com.grupocordillera.clientes.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.grupocordillera.clientes.controller.ClientesController;
import com.grupocordillera.clientes.exception.ClienteNotFoundException;
import com.grupocordillera.clientes.service.ClientesService;

class GlobalExceptionHandlerTest {
    private ClientesService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ClientesService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ClientesController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void clienteInexistenteRetorna404() throws Exception {
        when(service.obtenerPorId(99L)).thenThrow(new ClienteNotFoundException(99L));

        mvc.perform(get("/clientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Cliente no encontrado con id: 99"));
    }

    @Test
    void errorInesperadoNoExponeMensajeTecnico() throws Exception {
        when(service.obtenerPorId(99L)).thenThrow(new RuntimeException("jdbc:password=secreto-interno"));

        mvc.perform(get("/clientes/99"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error interno al procesar la solicitud"));
    }
}
