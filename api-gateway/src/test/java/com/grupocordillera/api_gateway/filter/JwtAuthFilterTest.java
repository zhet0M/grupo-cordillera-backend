package com.grupocordillera.api_gateway.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.grupocordillera.api_gateway.config.JwtService;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private static final String EMAIL_TEST = "test@grupocordillera.com";
    private static final String TOKEN_VALIDO = "header.payload.signature";

    // ==================== Helpers ====================

    private MockHttpServletRequest request(String method, String path, String authHeader) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod(method);
        req.setRequestURI(path);
        req.setServletPath(path);
        if (authHeader != null) {
            req.addHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return req;
    }

    private MockHttpServletResponse response() {
        return new MockHttpServletResponse();
    }

    private MockFilterChain chain() {
        return new MockFilterChain();
    }

    private void doFilter(MockHttpServletRequest req, MockHttpServletResponse res, MockFilterChain chain)
            throws ServletException, IOException {
        jwtAuthFilter.doFilterInternal(req, res, chain);
    }

    private void configurarTokenValido(String email, String rol) {
        when(jwtService.validarToken(TOKEN_VALIDO)).thenReturn(true);
        when(jwtService.extraerEmail(TOKEN_VALIDO)).thenReturn(email);
        when(jwtService.extraerRol(TOKEN_VALIDO)).thenReturn(rol);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String responseBody(MockHttpServletResponse res) {
        try {
            return res.getContentAsString(StandardCharsets.ISO_8859_1);
        } catch (Exception e) {
            return "";
        }
    }

    // =====================================================================
    // PARAMETERIZED 1: ADMIN_<dominio> solo escribe en su dominio
    //                  (y GET en todos los core)
    // =====================================================================

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        // rol                | method | path                | status
        "ADMIN_VENTAS         | POST   | /ventas              | 200",
        "ADMIN_VENTAS         | PUT    | /ventas/1            | 200",
        "ADMIN_VENTAS         | DELETE | /ventas/5            | 200",
        "ADMIN_VENTAS         | POST   | /inventario          | 403",
        "ADMIN_VENTAS         | PUT    | /finanzas/1          | 403",
        "ADMIN_VENTAS         | DELETE | /clientes/7          | 403",
        "ADMIN_INVENTARIO     | POST   | /inventario          | 200",
        "ADMIN_INVENTARIO     | PUT    | /inventario/3        | 200",
        "ADMIN_INVENTARIO     | DELETE | /inventario/9        | 200",
        "ADMIN_INVENTARIO     | POST   | /ventas              | 403",
        "ADMIN_INVENTARIO     | PUT    | /finanzas/1          | 403",
        "ADMIN_FINANZAS       | POST   | /finanzas            | 200",
        "ADMIN_FINANZAS       | PUT    | /finanzas/2          | 200",
        "ADMIN_FINANZAS       | DELETE | /finanzas/8          | 200",
        "ADMIN_FINANZAS       | POST   | /ventas              | 403",
        "ADMIN_FINANZAS       | DELETE | /clientes/1          | 403",
        "ADMIN_CLIENTES       | POST   | /clientes            | 200",
        "ADMIN_CLIENTES       | PUT    | /clientes/1          | 200",
        "ADMIN_CLIENTES       | DELETE | /clientes/4          | 200",
        "ADMIN_CLIENTES       | POST   | /ventas              | 403",
        "ADMIN_CLIENTES       | PUT    | /inventario/1        | 403",
    })
    void adminDominioSoloEscribeEnSuDominio(String rol, String method, String path, int statusEsperado)
            throws ServletException, IOException {
        configurarTokenValido(EMAIL_TEST, rol);
        MockHttpServletRequest req = request(method, path, bearer(TOKEN_VALIDO));
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(statusEsperado, res.getStatus());
    }

    // =====================================================================
    // PARAMETERIZED 2: EJECUTIVO / ANALISTA solo lectura en core dominios
    //                  (ventas, inventario, finanzas, clientes)
    // =====================================================================

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "EJECUTIVO   | GET  | /ventas        | 200",
        "EJECUTIVO   | GET  | /inventario    | 200",
        "EJECUTIVO   | GET  | /finanzas      | 200",
        "EJECUTIVO   | GET  | /clientes      | 200",
        "EJECUTIVO   | POST | /ventas        | 403",
        "EJECUTIVO   | PUT  | /inventario/1  | 403",
        "EJECUTIVO   | DELETE| /finanzas/9  | 403",
        "EJECUTIVO   | POST | /clientes      | 403",
        "ANALISTA    | GET  | /ventas        | 200",
        "ANALISTA    | GET  | /inventario    | 200",
        "ANALISTA    | GET  | /finanzas      | 200",
        "ANALISTA    | GET  | /clientes      | 200",
        "ANALISTA    | POST | /ventas        | 403",
        "ANALISTA    | PUT  | /inventario/1  | 403",
        "ANALISTA    | DELETE| /clientes/3  | 403",
    })
    void ejecutivoAnalistaSoloLecturaEnCore(String rol, String method, String path, int statusEsperado)
            throws ServletException, IOException {
        configurarTokenValido(EMAIL_TEST, rol);
        MockHttpServletRequest req = request(method, path, bearer(TOKEN_VALIDO));
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(statusEsperado, res.getStatus());
    }

    // =====================================================================
    // PARAMETERIZED 3: EJECUTIVO / ANALISTA acceso a alertas, kpis, reportes
    // =====================================================================

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "EJECUTIVO   | GET    | /alertas       | 200",
        "EJECUTIVO   | POST   | /alertas       | 200",
        "EJECUTIVO   | PUT    | /alertas/1     | 200",
        "EJECUTIVO   | GET    | /kpis          | 200",
        "EJECUTIVO   | GET    | /kpis/ventas   | 200",
        "EJECUTIVO   | GET    | /reportes      | 200",
        "EJECUTIVO   | POST   | /reportes/pdf  | 200",
        "ANALISTA    | GET    | /alertas       | 200",
        "ANALISTA    | POST   | /alertas       | 200",
        "ANALISTA    | GET    | /kpis          | 200",
        "ANALISTA    | GET    | /reportes      | 200",
        "ANALISTA    | POST   | /reportes/pdf  | 200",
    })
    void ejecutivoAnalistaAccedenModulosAnalitica(String rol, String method, String path, int statusEsperado)
            throws ServletException, IOException {
        configurarTokenValido(EMAIL_TEST, rol);
        MockHttpServletRequest req = request(method, path, bearer(TOKEN_VALIDO));
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(statusEsperado, res.getStatus());
    }

    // =====================================================================
    // PARAMETERIZED 4: SUPER_ADMIN tiene acceso TOTAL a TODO
    // =====================================================================

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "SUPER_ADMIN | GET    | /ventas            | 200",
        "SUPER_ADMIN | POST   | /ventas            | 200",
        "SUPER_ADMIN | PUT    | /inventario/1      | 200",
        "SUPER_ADMIN | DELETE | /finanzas/99       | 200",
        "SUPER_ADMIN | POST   | /clientes          | 200",
        "SUPER_ADMIN | GET    | /alertas           | 200",
        "SUPER_ADMIN | POST   | /alertas           | 200",
        "SUPER_ADMIN | GET    | /kpis              | 200",
        "SUPER_ADMIN | POST   | /kpis/recalcular   | 200",
        "SUPER_ADMIN | GET    | /reportes          | 200",
        "SUPER_ADMIN | POST   | /reportes/pdf      | 200",
        "SUPER_ADMIN | GET    | /auth/admin        | 200",
        "SUPER_ADMIN | POST   | /auth/admin/roles  | 200",
    })
    void superAdminAccesoTotalATodo(String rol, String method, String path, int statusEsperado)
            throws ServletException, IOException {
        configurarTokenValido(EMAIL_TEST, rol);
        MockHttpServletRequest req = request(method, path, bearer(TOKEN_VALIDO));
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(statusEsperado, res.getStatus());
    }

    // =====================================================================
    // PARAMETERIZED 5: /auth/admin/** SOLO ADMIN_USUARIOS y SUPER_ADMIN
    // =====================================================================

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "ADMIN_USUARIOS | GET    | /auth/admin           | 200",
        "ADMIN_USUARIOS | POST   | /auth/admin/usuarios  | 200",
        "ADMIN_USUARIOS | PUT    | /auth/admin/usuarios/5| 200",
        "SUPER_ADMIN    | GET    | /auth/admin           | 200",
        "SUPER_ADMIN    | DELETE | /auth/admin/roles/3   | 200",
        "ADMIN_VENTAS   | GET    | /auth/admin           | 403",
        "ADMIN_INVENTARIO| GET  | /auth/admin/usuarios   | 403",
        "ADMIN_FINANZAS | POST   | /auth/admin           | 403",
        "ADMIN_CLIENTES | GET    | /auth/admin           | 403",
        "EJECUTIVO      | GET    | /auth/admin           | 403",
        "ANALISTA       | GET    | /auth/admin           | 403",
    })
    void authAdminSoloAdminUsuariosSuperAdmin(String rol, String method, String path, int statusEsperado)
            throws ServletException, IOException {
        configurarTokenValido(EMAIL_TEST, rol);
        MockHttpServletRequest req = request(method, path, bearer(TOKEN_VALIDO));
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(statusEsperado, res.getStatus());
    }

    // =====================================================================
    // AUTENTICACIÓN (401): casos que NO llegan a autorización
    // =====================================================================

    @Test
    void requestSinAuthorizationHeaderRetorna401() throws ServletException, IOException {
        MockHttpServletRequest req = request("GET", "/ventas", null);
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        assertEquals("Token no proporcionado", responseBody(res));
        assertNull(chain.getRequest());
    }

    @Test
    void requestAuthorizationSinBearerRetorna401() throws ServletException, IOException {
        MockHttpServletRequest req = request("GET", "/ventas", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        assertEquals("Token no proporcionado", responseBody(res));
    }

    @Test
    void requestConSoloPalabraBearerSinTokenRetorna401() throws ServletException, IOException {
        // Bearer + espacio pero sin token → authHeader.length() < 7 + 1 → substring(7) devuelve ""
        MockHttpServletRequest req = request("GET", "/ventas", "Bearer ");
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        when(jwtService.validarToken("")).thenReturn(false);

        doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        assertEquals("Token inválido o expirado", responseBody(res));
    }

    @Test
    void requestConTokenMalFormadoRetorna401() throws ServletException, IOException {
        String token = "esto-no-es-un-jwt-valido";
        MockHttpServletRequest req = request("GET", "/ventas", bearer(token));
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        when(jwtService.validarToken(token)).thenReturn(false);

        doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        assertEquals("Token inválido o expirado", responseBody(res));
    }

    @Test
    void requestConTokenExpiradoRetorna401() throws ServletException, IOException {
        String token = "expired.jwt.token";
        MockHttpServletRequest req = request("GET", "/ventas", bearer(token));
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        when(jwtService.validarToken(token)).thenReturn(false);

        doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        assertTrue(responseBody(res).contains("invalido") || responseBody(res).contains("expirado"));
    }

    @Test
    void tokenValidoSinClaimRolDevuelveNullYSeAplicaReglaDefault() throws ServletException, IOException {
        //HALLAZGO: si el JWT es válido pero no trae claim "rol",
        // extraerRol() retorna null y tienePermiso() aplica reglas
        // sin cortocircuito:
        //  - GET /ventas → isGet=true → return true → PASA (200)
        //  - POST /ventas → ADMIN_VENTAS.equals(null) false → 403
        //  - GET /reportes → EJECUTIVO/ANALISTA equals null → 403
        //  - /auth/admin → false → 403
        // No hay un "rol default" ni rechazo explícito ante rol null.
        String token = TOKEN_VALIDO;
        MockHttpServletRequest reqGet = request("GET", "/ventas", bearer(token));
        MockHttpServletResponse resGet = response();
        MockFilterChain chainGet = chain();

        when(jwtService.validarToken(token)).thenReturn(true);
        when(jwtService.extraerEmail(token)).thenReturn(EMAIL_TEST);
        when(jwtService.extraerRol(token)).thenReturn(null);

        doFilter(reqGet, resGet, chainGet);

        assertEquals(200, resGet.getStatus(),
                "GET /ventas con rol null retorna 200 (isGet=true en el branch /ventas)");

        MockHttpServletRequest reqPost = request("POST", "/ventas", bearer(token));
        MockHttpServletResponse resPost = response();
        MockFilterChain chainPost = chain();
        doFilter(reqPost, resPost, chainPost);
        assertEquals(403, resPost.getStatus(),
                "POST /ventas con rol null retorna 403 (no coincide ADMIN_VENTAS)");

        MockHttpServletRequest reqReportes = request("GET", "/reportes", bearer(token));
        MockHttpServletResponse resReportes = response();
        MockFilterChain chainReportes = chain();
        doFilter(reqReportes, resReportes, chainReportes);
        assertEquals(403, resReportes.getStatus(),
                "GET /reportes con rol null retorna 403 (no es EJECUTIVO ni ANALISTA)");
    }

    // =====================================================================
    // RUTAS PÚBLICAS: no requieren JWT, pasan directamente
    // =====================================================================

    @ParameterizedTest
    @MethodSource("rutasPublicasProvider")
    void rutasPublicasPasanSinJwt(String method, String path) throws ServletException, IOException {
        MockHttpServletRequest req = request(method, path, null);
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(200, res.getStatus(),
                "Ruta pública " + method + " " + path + " debería pasar sin JWT (status 200 default)");
    }

    static Stream<String[]> rutasPublicasProvider() {
        return Stream.of(
                new String[]{"GET", "/auth/login"},
                new String[]{"POST", "/auth/login"},
                new String[]{"POST", "/auth/registro"},
                new String[]{"GET", "/auth/registro"},
                new String[]{"GET", "/swagger-ui.html"},
                new String[]{"GET", "/swagger-ui/index.html"},
                new String[]{"GET", "/swagger-ui/swagger-initializer.js"},
                new String[]{"GET", "/swagger-ui"},
                new String[]{"GET", "/v3/api-docs"},
                new String[]{"GET", "/v3/api-docs/ventas"},
                new String[]{"GET", "/v3/api-docs.yaml"},
                new String[]{"GET", "/webjars/swagger-ui/index.html"}
        );
    }

    // =====================================================================
    // HALLAZGOS en RBAC
    // =====================================================================

    @Test
    void reportesBloqueaATodosLosAdminDominioInclusoLectura() throws ServletException, IOException {
        //HALLAZGO: en tienePermiso() el branch de /reportes es:
        //   return "EJECUTIVO".equals(rol) || "ANALISTA".equals(rol);
        // Esto significa que los ADMIN_* (ADMIN_VENTAS, ADMIN_INVENTARIO,
        // ADMIN_FINANZAS, ADMIN_CLIENTES, ADMIN_USUARIOS) NO pueden acceder
        // a /reportes en NINGÚN método, ni siquiera lectura. El único Admin
        // que sí pasa es SUPER_ADMIN (por el cortocircuito inicial).
        // Difiere del comportamiento ideal: todos los usuarios autenticados
        // deberían poder descargar reportes al menos de su dominio.
        String[] adminBloqueados = {
                "ADMIN_VENTAS", "ADMIN_INVENTARIO", "ADMIN_FINANZAS",
                "ADMIN_CLIENTES", "ADMIN_USUARIOS"
        };

        for (String rol : adminBloqueados) {
            configurarTokenValido(EMAIL_TEST, rol);
            MockHttpServletRequest req = request("GET", "/reportes", bearer(TOKEN_VALIDO));
            MockHttpServletResponse res = response();
            MockFilterChain chain = chain();

            doFilter(req, res, chain);

            assertEquals(403, res.getStatus(),
                    "HALLAZGO confirmado: rol=" + rol + " es rechazado en /reportes");
        }
    }

    @Test
    void kpisEscrituraBloqueadoInclusoAUsuariosAdmin() throws ServletException, IOException {
        //HALLAZGO: branch /kpis en tienePermiso:
        //   if (isGet) return true;  return false;
        // Entonces POST/PUT/DELETE en /kpis retorna false para todos salvo
        // SUPER_ADMIN (que se salva por el primer if). ADMIN_* no pueden
        // recalcular KPIs ni mutar endpoints de /kpis.
        String[] rolesSinEscritura = {
                "ADMIN_VENTAS", "ADMIN_INVENTARIO", "ADMIN_FINANZAS",
                "ADMIN_CLIENTES", "ADMIN_USUARIOS", "EJECUTIVO", "ANALISTA"
        };

        for (String rol : rolesSinEscritura) {
            configurarTokenValido(EMAIL_TEST, rol);
            MockHttpServletRequest req = request("POST", "/kpis/recalcular", bearer(TOKEN_VALIDO));
            MockHttpServletResponse res = response();
            MockFilterChain chain = chain();

            doFilter(req, res, chain);

            assertEquals(403, res.getStatus(),
                    "HALLAZGO confirmado: rol=" + rol + " no puede POST /kpis");
        }

        configurarTokenValido(EMAIL_TEST, "SUPER_ADMIN");
        MockHttpServletRequest reqSuper = request("POST", "/kpis/recalcular", bearer(TOKEN_VALIDO));
        MockHttpServletResponse resSuper = response();
        MockFilterChain chainSuper = chain();
        doFilter(reqSuper, resSuper, chainSuper);
        assertEquals(200, resSuper.getStatus(), "SUPER_ADMIN sí pasa por escritura en /kpis");
    }

    @Test
    void pathDesconocidoRetornaTruePasaSinRestriccion() throws ServletException, IOException {
        //HALLAZGO: al final de tienePermiso() hay un `return true;` de
        // cortesía. Cualquier ruta que no coincida con /ventas, /inventario,
        // /finanzas, /clientes, /kpis, /reportes, /alertas, /auth/admin
        // PASA AUTOMÁTICAMENTE con cualquier método y cualquier rol (incluso
        // roles inexistentes). Es una puerta trasera potencial: si alguien
        // agrega un nuevo endpoint en algún microservicio y se olvida de
        // declararlo aquí, queda expuesto sin control RBAC.
        String rutaDesconocida = "/modulo-secreto-no-declarado/operacion";
        String[] rolesCualquiera = {"EJECUTIVO", "ROL_INEXISTENTE", "INVITADO"};
        for (String rol : rolesCualquiera) {
            configurarTokenValido(EMAIL_TEST, rol);
            MockHttpServletRequest req = request("POST", rutaDesconocida, bearer(TOKEN_VALIDO));
            MockHttpServletResponse res = response();
            MockFilterChain chain = chain();

            doFilter(req, res, chain);

            assertEquals(200, res.getStatus(),
                    "HALLAZGO confirmado: POST " + rutaDesconocida + " con rol=" + rol + " PASA");
        }
    }

    // =====================================================================
    // CORS / OPTIONS: preflight pasa sin autenticación
    // =====================================================================

    @Test
    void metodoOptionsPreflightPasaSinJwtNiPermisos() throws ServletException, IOException {
        MockHttpServletRequest req = request("OPTIONS", "/ventas", null);
        req.addHeader("Origin", "http://localhost:4200");
        req.addHeader("Access-Control-Request-Method", "POST");
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
    }

    @Test
    void optionsConOrigenExternoPasaDirecto() throws ServletException, IOException {
        MockHttpServletRequest req = request("OPTIONS", "/auth/admin/usuarios", null);
        req.addHeader("Origin", "https://frontend-grupo-cordillera-angular.vercel.app");
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
    }

    @Test
    void filtroPropagaAtributosEmailYRolEnRequest() throws ServletException, IOException {
        String email = "jefe.ventas@grupocordillera.com";
        String rol = "ADMIN_VENTAS";
        configurarTokenValido(email, rol);
        MockHttpServletRequest req = request("GET", "/ventas", bearer(TOKEN_VALIDO));
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(email, req.getAttribute("email"));
        assertEquals(rol, req.getAttribute("rol"));
    }

    @Test
    void mensaje403InformaFaltaDePermisos() throws ServletException, IOException {
        configurarTokenValido(EMAIL_TEST, "ADMIN_VENTAS");
        MockHttpServletRequest req = request("POST", "/inventario", bearer(TOKEN_VALIDO));
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(403, res.getStatus());
        assertEquals("No tienes permisos para acceder a este recurso", responseBody(res));
    }

    @Test
    void requestConRutaPublicaAntePonePublicaSobreJwt() throws ServletException, IOException {
        //HALLAZGO: RUTAS_PUBLICAS se evalúan ANTES que el header JWT.
        // Incluso si alguien envía un token INVÁLIDO a /auth/login, el
        // filtro deja pasar la request porque coincide con startsWith
        // ruta pública y no valida el token. Es decir: las rutas públicas
        // nunca verifican el token, incluso si viene uno.
        MockHttpServletRequest req = request("POST", "/auth/login", bearer("token.tremendamente.invalido"));
        MockHttpServletResponse res = response();
        MockFilterChain chain = chain();

        doFilter(req, res, chain);

        assertEquals(200, res.getStatus(),
                "Ruta pública /auth/login NO valida el token JWT, pasa directo");
    }
}
