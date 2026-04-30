package com.autenticacion.authentication.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.autenticacion.authentication.DTO.LoginRequest;
import com.autenticacion.authentication.DTO.LoginResponse;
import com.autenticacion.authentication.DTO.RegistroRequest;
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
    public List<Usuario> listarPendientes(){
        return usuarioRepository.findByEstado(Usuario.Estado.PENDIENTE);
    }

    public List<Usuario> listarTodo(){
        return usuarioRepository.findAll();
    }
    
    public Usuario aprobarUsuario(Long id, String rol){
        Usuario usuario = usuarioRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setEstado(Usuario.Estado.APROBADO);
        usuario.setRol(Usuario.Rol.valueOf(rol.toUpperCase()));

        return usuarioRepository.save(usuario);
    }

    public Usuario rechazarUsuario(Long id){
        Usuario usuario = usuarioRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        usuario.setEstado(Usuario.Estado.RECHAZADO);

        return usuarioRepository.save(usuario);
    }

    public Usuario bloquearUsuario(Long id){
        Usuario usuario = usuarioRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setEstado(Usuario.Estado.BLOQUEADO);

        return usuarioRepository.save(usuario);
    }
}
