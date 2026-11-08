package com.grupocordillera.api_gateway.filter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.grupocordillera.api_gateway.config.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterSecurityTest {

    private static final String TOKEN = "token-de-prueba";
    private static final String EMAIL = "qa@grupocordillera.com";

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthFilter filter;

    // INC-01: unknown routes must not become authorized automatically.
    @ParameterizedTest
    @ValueSource(strings = {"SUPER_ADMIN", "ADMIN_USUARIOS", "ADMIN_VENTAS", "ADMIN_INVENTARIO",
            "ADMIN_FINANZAS", "ADMIN_CLIENTES", "EJECUTIVO", "ANALISTA"})
    void rutaNoDeclaradaSeDeniegaParaCualquierRol(String rol) throws Exception {
        configurarToken(EMAIL, rol);
        assertDenegada("POST", "/nuevo-modulo/operacion", true, 403);
    }

    // INC-04: a signed token without a recognized role is not a usable identity.
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "INVITADO", "ROL_INEXISTENTE", "admin_ventas"})
    void rolAusenteODesconocidoSeRechaza(String rol) throws Exception {
        configurarToken(EMAIL, rol);
        assertDenegada("GET", "/ventas", true, 401);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void sujetoAusenteSeRechaza(String email) throws Exception {
        configurarToken(email, "ANALISTA");
        assertDenegada("GET", "/clientes", true, 401);
    }

    // Public routes are exact endpoints or documented path segments, not arbitrary prefixes.
    @ParameterizedTest
    @ValueSource(strings = {"/auth/login-extra", "/auth/login/admin", "/auth/registro-extra",
            "/swagger-ui.html-extra", "/swagger-ui-malicioso", "/v3/api-docs-extra",
            "/webjars-malicioso/recurso.js"})
    void rutaParecidaAPublicaNoOmiteAutenticacion(String path) throws Exception {
        assertDenegada("GET", path, false, 401);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/ventas-extra", "/inventario-extra", "/finanzas-extra", "/clientes-extra",
            "/kpis-extra", "/reportes-extra", "/alertas-extra", "/auth/admin-extra"})
    void prefijoParecidoADominioNoHeredaPermisos(String path) throws Exception {
        configurarToken(EMAIL, "SUPER_ADMIN");
        assertDenegada("GET", path, true, 403);
    }

    @Test
    void errorAlLeerClaimsNoSePropagaComoErrorInterno() throws Exception {
        when(jwtService.validarToken(TOKEN)).thenReturn(true);
        when(jwtService.extraerEmail(TOKEN)).thenThrow(new JwtException("detalle interno del parser"));
        assertDenegada("GET", "/ventas", true, 401);
    }

    private void configurarToken(String email, String rol) {
        when(jwtService.validarToken(TOKEN)).thenReturn(true);
        when(jwtService.extraerEmail(TOKEN)).thenReturn(email);
        when(jwtService.extraerRol(TOKEN)).thenReturn(rol);
    }

    private void assertDenegada(String method, String path, boolean autenticar, int expectedStatus)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (autenticar) {
            request.addHeader("Authorization", "Bearer " + TOKEN);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(expectedStatus, response.getStatus());
        assertNull(chain.getRequest(), "Una petición denegada no debe llegar al siguiente componente");
        assertNull(request.getAttribute("email"));
        assertNull(request.getAttribute("rol"));
    }
}
