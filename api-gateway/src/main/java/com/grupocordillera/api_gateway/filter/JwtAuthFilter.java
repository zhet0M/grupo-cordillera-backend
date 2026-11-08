package com.grupocordillera.api_gateway.filter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.grupocordillera.api_gateway.config.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    // Rutas públicas que no necesitan token
    private static final List<String> RUTAS_PUBLICAS = List.of(
            "/auth/login",
            "/auth/registro",
            "/swagger-ui.html",
            "/v3/api-docs.yaml"
    );

    private static final List<String> RUTAS_DOCUMENTACION = List.of(
            "/swagger-ui", "/v3/api-docs", "/webjars");

    private static final Set<String> ROLES_VALIDOS = Set.of(
            "SUPER_ADMIN", "ADMIN_USUARIOS", "ADMIN_VENTAS", "ADMIN_INVENTARIO",
            "ADMIN_FINANZAS", "ADMIN_CLIENTES", "EJECUTIVO", "ANALISTA");

    private static final List<String> DOMINIOS_AUTORIZADOS = List.of(
            "/ventas", "/inventario", "/finanzas", "/clientes", "/kpis",
            "/reportes", "/alertas", "/auth/admin");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Si es ruta pública, dejar pasar
        if (RUTAS_PUBLICAS.contains(path)
                || RUTAS_DOCUMENTACION.stream().anyMatch(ruta -> perteneceADominio(path, ruta))) {
            filterChain.doFilter(request, response);
            return;
        }

        // Obtener el header Authorization
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Verifica que venga el token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token no proporcionado");
            return;
        }

        // Extrae y valida el token
        String token = authHeader.substring(7);

        String email;
        String rol;
        try {
            if (!jwtService.validarToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token inválido o expirado");
                return;
            }
            email = jwtService.extraerEmail(token);
            rol = jwtService.extraerRol(token);
        } catch (JwtException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token inválido o expirado");
            return;
        }

        if (email == null || email.isBlank() || rol == null || !ROLES_VALIDOS.contains(rol)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token inválido o expirado");
            return;
        }

        if (!tienePermiso(path, method, rol)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("No tienes permisos para acceder a este recurso");
            return;
        }

        request.setAttribute("email", email);
        request.setAttribute("rol", rol);

        filterChain.doFilter(request, response);
    }

    private boolean tienePermiso(String path, String method, String rol) {
        if (DOMINIOS_AUTORIZADOS.stream().noneMatch(dominio -> perteneceADominio(path, dominio))) {
            return false;
        }

        if ("SUPER_ADMIN".equals(rol)) {
            return true;
        }

        boolean isGet = "GET".equalsIgnoreCase(method);

        if (perteneceADominio(path, "/ventas")) {
            if (isGet) return true;
            return "ADMIN_VENTAS".equals(rol);
        }
        if (perteneceADominio(path, "/inventario")) {
            if (isGet) return true;
            return "ADMIN_INVENTARIO".equals(rol);
        }
        if (perteneceADominio(path, "/finanzas")) {
            if (isGet) return true;
            return "ADMIN_FINANZAS".equals(rol);
        }
        if (perteneceADominio(path, "/clientes")) {
            if (isGet) return true;
            return "ADMIN_CLIENTES".equals(rol);
        }
        if (perteneceADominio(path, "/kpis")) {
            if (isGet) return true;
            return false;
        }
        if (perteneceADominio(path, "/reportes")) {
            return "EJECUTIVO".equals(rol) || "ANALISTA".equals(rol);
        }
        if (perteneceADominio(path, "/alertas")) {
            if (isGet) return true;
            return "EJECUTIVO".equals(rol) || "ANALISTA".equals(rol);
        }
        
        if (perteneceADominio(path, "/auth/admin")) {
            return "ADMIN_USUARIOS".equals(rol) || "SUPER_ADMIN".equals(rol);
        }

        return false;
    }

    private boolean perteneceADominio(String path, String dominio) {
        return path.equals(dominio) || path.startsWith(dominio + "/");
    }
}
