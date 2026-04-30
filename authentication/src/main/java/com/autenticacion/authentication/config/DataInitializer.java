package com.autenticacion.authentication.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.autenticacion.authentication.model.Usuario;
import com.autenticacion.authentication.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner{

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args){
        if(usuarioRepository.findByEmail("admin@grupocordillera.com").isEmpty()){
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setEmail("admin@grupocordillera.com");
            admin.setPassword(passwordEncoder.encode("admin1234"));
            admin.setRol(Usuario.Rol.SUPER_ADMIN);
            admin.setEstado(Usuario.Estado.APROBADO);
            usuarioRepository.save(admin);
            System.out.println("Usuario admin creado correctamente");
        }
    }

    
}
