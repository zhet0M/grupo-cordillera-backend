package com.grupocordillera.api_gateway.filter;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.grupocordillera.api_gateway.config.JwtService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

// Component security tests: real JWT signing/parsing + real filter, without a live server.
class GatewayJwtSecurityTest {

    private JwtService service;
    private JwtAuthFilter filter;
    private Key signingKey;

    @BeforeEach
    void setUp() {
        String secret = UUID.randomUUID().toString() + UUID.randomUUID();
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        service.validarConfiguracion();
        filter = new JwtAuthFilter(service);
    }

    @Test
    void tokenFirmadoValidoPermiteLectura() throws Exception {
        assertRespuesta(token("qa@grupocordillera.com", "ANALISTA", Instant.now().plusSeconds(300), signingKey), 200);
    }

    @Test
    void tokenFirmadoConOtraClaveSeRechaza() throws Exception {
        Key otherKey = Keys.hmacShaKeyFor((UUID.randomUUID().toString() + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
        assertRespuesta(token("qa@grupocordillera.com", "SUPER_ADMIN", Instant.now().plusSeconds(300), otherKey), 401);
    }

    @Test
    void tokenExpiradoSeRechaza() throws Exception {
        assertRespuesta(token("qa@grupocordillera.com", "ANALISTA", Instant.EPOCH, signingKey), 401);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "INVITADO"})
    void tokenFirmadoSinRolReconocidoSeRechaza(String rol) throws Exception {
        assertRespuesta(token("qa@grupocordillera.com", rol, Instant.now().plusSeconds(300), signingKey), 401);
    }

    @Test
    void tokenFirmadoSinSujetoSeRechaza() throws Exception {
        assertRespuesta(token(null, "SUPER_ADMIN", Instant.now().plusSeconds(300), signingKey), 401);
    }

    @Test
    void tipoDeClaimIncorrectoSeRechazaSinExponerExcepcion() throws Exception {
        assertRespuesta(token("qa@grupocordillera.com", 123, Instant.now().plusSeconds(300), signingKey), 401);
    }

    @Test
    void tokenVacioSeRechazaSinErrorInterno() throws Exception {
        assertRespuesta("", 401);
    }

    @Test
    void claveDebilImpideInicializacion() {
        ReflectionTestUtils.setField(service, "secret", "corta");
        assertThrows(WeakKeyException.class, service::validarConfiguracion);
    }

    private String token(String email, Object rol, Instant expiration, Key key) {
        return Jwts.builder().setSubject(email).claim("rol", rol)
                .setExpiration(Date.from(expiration)).signWith(key).compact();
    }

    private void assertRespuesta(String token, int status) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ventas");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(status, response.getStatus());
        if (status == 200) {
            assertNotNull(chain.getRequest());
            assertEquals("ANALISTA", request.getAttribute("rol"));
        } else {
            assertNull(chain.getRequest());
            assertEquals("Token inválido o expirado", response.getContentAsString());
        }
    }
}
