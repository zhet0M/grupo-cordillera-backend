package com.autenticacion.authentication.config;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.autenticacion.authentication.model.Usuario;
import com.autenticacion.authentication.repository.UsuarioRepository;

class DataInitializerTest {

    private UsuarioRepository repository;
    private BCryptPasswordEncoder encoder;
    private DataInitializer initializer;
    private String password;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        encoder = new BCryptPasswordEncoder();
        initializer = new DataInitializer(repository, encoder);
        password = UUID.randomUUID().toString();
        ReflectionTestUtils.setField(initializer, "adminEmail", " QA.ADMIN@grupocordillera.com ");
        ReflectionTestUtils.setField(initializer, "adminUsername", " qa-admin ");
        ReflectionTestUtils.setField(initializer, "adminPassword", password);
    }

    @Test
    void creaAdministradorConfiguradoConBcryptReal() {
        when(repository.findByEmail("qa.admin@grupocordillera.com")).thenReturn(Optional.empty());

        initializer.run();

        ArgumentCaptor<Usuario> saved = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(saved.capture());
        Usuario admin = saved.getValue();
        assertEquals("qa-admin", admin.getUsername());
        assertEquals("qa.admin@grupocordillera.com", admin.getEmail());
        assertEquals(Usuario.Rol.SUPER_ADMIN, admin.getRol());
        assertEquals(Usuario.Estado.APROBADO, admin.getEstado());
        assertNotEquals(password, admin.getPassword());
        assertTrue(encoder.matches(password, admin.getPassword()));
    }

    @Test
    void noSobrescribeUsuarioExistenteNiSuPassword() {
        Usuario existing = new Usuario();
        existing.setPassword("hash-existente");
        when(repository.findByEmail("qa.admin@grupocordillera.com")).thenReturn(Optional.of(existing));

        initializer.run();

        verify(repository, never()).save(any());
        assertEquals("hash-existente", existing.getPassword());
    }

    @ParameterizedTest
    @ValueSource(strings = {"adminEmail", "adminUsername", "adminPassword"})
    void configuracionIncompletaNoCreaUsuario(String field) {
        ReflectionTestUtils.setField(initializer, field, " ");

        assertThrows(IllegalStateException.class, () -> initializer.run());

        verify(repository, never()).save(any());
    }

    @Test
    void inicializacionEstaDesactivadaPorDefecto() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(DataInitializer.class);
            context.refresh();

            assertEquals(0, context.getBeanNamesForType(DataInitializer.class).length);
        }
    }
}
