package com.autenticacion.authentication.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autenticacion.authentication.DTO.AprobarRequest;
import com.autenticacion.authentication.DTO.LoginRequest;
import com.autenticacion.authentication.DTO.LoginResponse;
import com.autenticacion.authentication.DTO.RegistroRequest;
import com.autenticacion.authentication.model.Usuario;
import com.autenticacion.authentication.service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/auth")
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
    public ResponseEntity<List<Usuario>> pendientes(){
        return ResponseEntity.ok(authService.listarPendientes());    
    }

    @GetMapping("/admin/usuarios")
    public ResponseEntity<List<Usuario>> listarTodo(){
        return ResponseEntity.ok(authService.listarTodo());
    }

    @PutMapping("/admin/usuarios/{id}/aprobar")
    public ResponseEntity<Usuario> aprobar(
        @PathVariable Long id,
        @RequestBody AprobarRequest request) {
            return ResponseEntity.ok(authService.aprobarUsuario(id, request.getRol()));
    }

    @PutMapping("/admin/usuarios/{id}/rechazar")
    public ResponseEntity<Usuario> rechazar(
        @PathVariable Long id) {
            return ResponseEntity.ok(authService.rechazarUsuario(id));
    }

    @PutMapping("/admin/usuarios/{id}/bloquear")
    public ResponseEntity<Usuario> bloquear(
        @PathVariable Long id){
            return ResponseEntity.ok(authService.bloquearUsuario(id));
    }
    

}
