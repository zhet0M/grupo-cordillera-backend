package com.grupocordillera.api_gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.grupocordillera.api_gateway.config.GatewayConfig;
import com.grupocordillera.api_gateway.config.JwtService;

import jakarta.servlet.ServletException;

class SecurityHeadersFilterTest {
    @ParameterizedTest
    @CsvSource({"GET,/clientes,true,200", "GET,/clientes,false,401",
            "POST,/clientes,true,403", "GET,/reportes/ventas/pdf,true,200"})
    void cabecerasSeAplicanARespuestasAutorizadasYDenegadas(String method, String path,
            boolean authenticated, int expectedStatus) throws Exception {
        JwtService jwt = mock(JwtService.class);
        if (authenticated) {
            when(jwt.validarToken("token-qa")).thenReturn(true);
            when(jwt.extraerEmail("token-qa")).thenReturn("qa@grupocordillera.com");
            when(jwt.extraerRol("token-qa")).thenReturn("ANALISTA");
        }
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (authenticated) request.addHeader("Authorization", "Bearer token-qa");
        MockHttpServletResponse response = new MockHttpServletResponse();
        // The terminal servlet is only reached when authorization allows it.
        MockFilterChain chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
            @Override protected void service(jakarta.servlet.http.HttpServletRequest req,
                    jakarta.servlet.http.HttpServletResponse res) { res.setStatus(200); }
        }, new JwtAuthFilter(jwt));

        new SecurityHeadersFilter().doFilter(request, response, chain);

        assertEquals(expectedStatus, response.getStatus());
        assertHeaders(response);
    }

    @Test
    void preflightDeOrigenPermitidoConservaCors() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/clientes");
        request.addHeader("Origin", "http://localhost:4200");
        request.addHeader("Access-Control-Request-Method", "GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        var cors = new GatewayConfig(null).corsFilter().getFilter();

        new SecurityHeadersFilter().doFilter(request, response,
                new MockFilterChain(new jakarta.servlet.http.HttpServlet() {}, cors));

        assertEquals(200, response.getStatus());
        assertEquals("http://localhost:4200", response.getHeader("Access-Control-Allow-Origin"));
        assertHeaders(response);
    }

    @Test
    void origenAjenoNoRecibePermisoCors() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/clientes");
        request.addHeader("Origin", "https://origen-no-permitido.invalid");
        request.addHeader("Access-Control-Request-Method", "GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        var cors = new GatewayConfig(null).corsFilter().getFilter();

        new SecurityHeadersFilter().doFilter(request, response,
                new MockFilterChain(new jakarta.servlet.http.HttpServlet() {}, cors));

        assertEquals(403, response.getStatus());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        assertHeaders(response);
    }

    private void assertHeaders(MockHttpServletResponse response) {
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("same-origin", response.getHeader("Cross-Origin-Resource-Policy"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }
}
