package com.autenticacion.authentication.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autenticacion.authentication.DTO.AprobarRequest;
import com.autenticacion.authentication.DTO.LoginRequest;
import com.autenticacion.authentication.DTO.LoginResponse;
import com.autenticacion.authentication.DTO.RegistroRequest;
import com.autenticacion.authentication.DTO.UsuarioAdminResponse;
import com.autenticacion.authentication.service.AuthService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión y administración de usuarios")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // endpoinst publicos

    @PostMapping("/registro")
    public ResponseEntity<String> register(@RequestBody RegistroRequest request){
        authService.registrar(request);
        return ResponseEntity.ok("Usuario registrado correctamente");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }

    // endpoinst admin

    @GetMapping("/admin/pendientes")
    public ResponseEntity<List<UsuarioAdminResponse>> pendientes(){
        return ResponseEntity.ok(authService.listarPendientes());    
    }

    @GetMapping("/admin/usuarios")
    public ResponseEntity<List<UsuarioAdminResponse>> listarTodo(){
        return ResponseEntity.ok(authService.listarTodo());
    }

    @GetMapping("/admin/roles")
    public ResponseEntity<List<String>> listarRoles(Authentication authentication){
        return ResponseEntity.ok(authService.rolesDisponibles(obtenerRolActual(authentication)));
    }

    @PutMapping("/admin/usuarios/{id}/aprobar")
    public ResponseEntity<UsuarioAdminResponse> aprobar(
        @PathVariable Long id,
        @RequestBody AprobarRequest request,
        Authentication authentication) {
            return ResponseEntity.ok(authService.aprobarUsuario(id, request.getRol(), obtenerRolActual(authentication)));
    }

    @PutMapping("/admin/usuarios/{id}/rechazar")
    public ResponseEntity<UsuarioAdminResponse> rechazar(
        @PathVariable Long id,
        Authentication authentication) {
            return ResponseEntity.ok(authService.rechazarUsuario(id, obtenerRolActual(authentication)));
    }

    @PutMapping("/admin/usuarios/{id}/bloquear")
    public ResponseEntity<UsuarioAdminResponse> bloquear(
        @PathVariable Long id,
        Authentication authentication){
            return ResponseEntity.ok(authService.bloquearUsuario(id, obtenerRolActual(authentication)));
    }

    @PutMapping("/admin/usuarios/{id}/desbloquear")
    public ResponseEntity<UsuarioAdminResponse> desbloquear(
        @PathVariable Long id,
        Authentication authentication){
            return ResponseEntity.ok(authService.desbloquearUsuario(id, obtenerRolActual(authentication)));
    }

    private String obtenerRolActual(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(authority -> authority.getAuthority())
                .orElseThrow(() -> new RuntimeException("Rol no disponible"));
    }

}
