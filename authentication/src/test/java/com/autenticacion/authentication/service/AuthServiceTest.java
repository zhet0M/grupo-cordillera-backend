package com.autenticacion.authentication.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.autenticacion.authentication.DTO.LoginRequest;
import com.autenticacion.authentication.DTO.RegistroRequest;
import com.autenticacion.authentication.model.Usuario;
import com.autenticacion.authentication.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "dominioPermitido", "@grupocordillera.com");
    }

    @Test
    void registrarUsuarioCorporativoGuardaUsuarioPendiente() {
        when(usuarioRepository.findByUsername("juan")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("1234")).thenReturn("encoded-password");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistroRequest request = new RegistroRequest();
        request.setUsername("juan");
        request.setEmail("juan@grupocordillera.com");
        request.setPassword("1234");

        Usuario resultado = authService.registrar(request);

        assertEquals("juan", resultado.getUsername());
        assertEquals("juan@grupocordillera.com", resultado.getEmail());
        assertEquals("encoded-password", resultado.getPassword());
        assertEquals(Usuario.Estado.PENDIENTE, resultado.getEstado());
        verify(usuarioRepository, times(2)).save(any(Usuario.class));
    }

    @Test
    void registrarUsuarioConCorreoNoCorporativoFalla() {
        RegistroRequest request = new RegistroRequest();
        request.setUsername("juan");
        request.setEmail("juan@gmail.com");
        request.setPassword("1234");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.registrar(request));

        assertEquals("Solo se permiten correos corporativos: @grupocordillera.com", ex.getMessage());
    }

    @Test
    void loginExitosoDevuelveTokenYRol() {
        Usuario usuario = new Usuario();
        usuario.setEmail("admin@grupocordillera.com");
        usuario.setUsername("admin");
        usuario.setPassword("encoded");
        usuario.setEstado(Usuario.Estado.APROBADO);
        usuario.setRol(Usuario.Rol.SUPER_ADMIN);

        when(usuarioRepository.findByEmail("admin@grupocordillera.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "encoded")).thenReturn(true);
        when(jwtService.generarToken("admin@grupocordillera.com", "SUPER_ADMIN")).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest();
        request.setEmail("admin@grupocordillera.com");
        request.setPassword("1234");

        var response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("admin", response.getUsername());
        assertEquals("admin@grupocordillera.com", response.getEmail());
        assertEquals("SUPER_ADMIN", response.getRol());
    }

    @Test
    void loginConPasswordIncorrectaDevuelveCredencialesInvalidas() {
        Usuario usuario = new Usuario();
        usuario.setEmail("user@grupocordillera.com");
        usuario.setUsername("user");
        usuario.setPassword("encoded");
        usuario.setEstado(Usuario.Estado.APROBADO);
        usuario.setRol(Usuario.Rol.ANALISTA);

        when(usuarioRepository.findByEmail("user@grupocordillera.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("bad-pass", "encoded")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("user@grupocordillera.com");
        request.setPassword("bad-pass");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));

        assertEquals("Correo o contraseña incorrectos", ex.getMessage());
    }
}
