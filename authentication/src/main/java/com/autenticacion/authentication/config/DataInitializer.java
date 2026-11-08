package com.autenticacion.authentication.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.autenticacion.authentication.model.Usuario;
import com.autenticacion.authentication.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner{

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap-admin.username:}")
    private String adminUsername;

    @Value("${app.bootstrap-admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args){
        if (adminEmail == null || adminEmail.isBlank() || adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("La inicialización administrativa requiere email, username y password configurados");
        }
        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        if(usuarioRepository.findByEmail(email).isEmpty()){
            Usuario admin = new Usuario();
            admin.setUsername(adminUsername.trim());
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRol(Usuario.Rol.SUPER_ADMIN);
            admin.setEstado(Usuario.Estado.APROBADO);
            usuarioRepository.save(admin);
        }
    }

    
}
