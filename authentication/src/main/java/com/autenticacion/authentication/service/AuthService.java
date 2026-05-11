package com.autenticacion.authentication.service;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.autenticacion.authentication.DTO.LoginRequest;
import com.autenticacion.authentication.DTO.LoginResponse;
import com.autenticacion.authentication.DTO.RegistroRequest;
import com.autenticacion.authentication.DTO.UsuarioAdminResponse;
import com.autenticacion.authentication.model.Usuario;
import com.autenticacion.authentication.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.dominio-permitido}")
    private String dominioPermitido;

    //Registro
    public Usuario registrar(RegistroRequest request){
        
        //Validacion dominio email
        if (!request.getEmail().endsWith(dominioPermitido)){
            throw new RuntimeException("Solo se permiten correos corporativos: " + dominioPermitido);
        }


        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()){
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setEstado(Usuario.Estado.PENDIENTE);
        usuario.setRol(null);
        usuarioRepository.save(usuario);

        return usuarioRepository.save(usuario);
    }

    public LoginResponse login(LoginRequest request){
        Usuario usuario = usuarioRepository
                            .findByEmail(request.getEmail())
                            .orElseThrow(()-> new RuntimeException("Usuario no encontrado"));
        
        switch (usuario.getEstado()) {
            case PENDIENTE -> throw new RuntimeException("Tu cuenta esta pendiente de aprobación");
            case RECHAZADO -> throw new RuntimeException("Tu cuenta fue rechazada");
            case BLOQUEADO -> throw new RuntimeException("Tu cuenta fue bloqueada");
            default -> {  }
            
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())){
            throw new RuntimeException("Contraseña incorrecta");
        }

        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol().name());
        
        return new LoginResponse(
            token,
            usuario.getUsername(),
            usuario.getEmail(),
            usuario.getRol().name()
        );

    }

    // admin
    public List<UsuarioAdminResponse> listarPendientes(){
        return usuarioRepository.findByEstado(Usuario.Estado.PENDIENTE)
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    public List<UsuarioAdminResponse> listarTodo(){
        return usuarioRepository.findAll()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }
    
    public UsuarioAdminResponse aprobarUsuario(Long id, String rol, String actorRol){
        Usuario usuario = usuarioRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Usuario.Rol nuevoRol = parseRol(rol);
        validarAsignacionRol(actorRol, nuevoRol);

        usuario.setEstado(Usuario.Estado.APROBADO);
        usuario.setRol(nuevoRol);

        return toAdminResponse(usuarioRepository.save(usuario));
    }

    public UsuarioAdminResponse rechazarUsuario(Long id, String actorRol){
        Usuario usuario = usuarioRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (usuario.getRol() == Usuario.Rol.SUPER_ADMIN && !"SUPER_ADMIN".equals(actorRol)) {
            throw new RuntimeException("No tienes permisos para rechazar a un SUPER_ADMIN");
        }

        usuario.setEstado(Usuario.Estado.RECHAZADO);

        return toAdminResponse(usuarioRepository.save(usuario));
    }

    public UsuarioAdminResponse bloquearUsuario(Long id, String actorRol){
        Usuario usuario = usuarioRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.getRol() == Usuario.Rol.SUPER_ADMIN && !"SUPER_ADMIN".equals(actorRol)) {
            throw new RuntimeException("No tienes permisos para bloquear a un SUPER_ADMIN");
        }

        usuario.setEstado(Usuario.Estado.BLOQUEADO);

        return toAdminResponse(usuarioRepository.save(usuario));
    }

    public List<String> rolesDisponibles(String actorRol) {
        if ("SUPER_ADMIN".equals(actorRol)) {
            return List.of(
                "ADMIN_USUARIOS",
                "ADMIN_VENTAS",
                "ADMIN_INVENTARIO",
                "ADMIN_FINANZAS",
                "ADMIN_CLIENTES",
                "EJECUTIVO",
                "ANALISTA",
                "SUPER_ADMIN"
            );
        }

        if ("ADMIN_USUARIOS".equals(actorRol)) {
            return List.of(
                "ADMIN_VENTAS",
                "ADMIN_INVENTARIO",
                "ADMIN_FINANZAS",
                "ADMIN_CLIENTES",
                "EJECUTIVO",
                "ANALISTA"
            );
        }

        throw new RuntimeException("No tienes permisos para gestionar roles");
    }

    private UsuarioAdminResponse toAdminResponse(Usuario usuario) {
        return UsuarioAdminResponse.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .email(usuario.getEmail())
                .rol(usuario.getRol() != null ? usuario.getRol().name() : null)
                .estado(usuario.getEstado().name())
                .build();
    }

    private Usuario.Rol parseRol(String rol) {
        try {
            return Usuario.Rol.valueOf(rol.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new RuntimeException("Rol invalido");
        }
    }

    private void validarAsignacionRol(String actorRol, Usuario.Rol nuevoRol) {
        List<String> rolesPermitidos = rolesDisponibles(actorRol);

        if (!rolesPermitidos.contains(nuevoRol.name())) {
            throw new RuntimeException("No tienes permisos para asignar este rol");
        }
    }
}
