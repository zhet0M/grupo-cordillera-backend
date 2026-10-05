package com.autenticacion.authentication.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.autenticacion.authentication.DTO.LoginRequest;
import com.autenticacion.authentication.DTO.LoginResponse;
import com.autenticacion.authentication.DTO.RegistroRequest;
import com.autenticacion.authentication.DTO.UsuarioAdminResponse;
import com.autenticacion.authentication.model.Usuario;
import com.autenticacion.authentication.repository.UsuarioRepository;

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

    // ==================== RF-2.1: Registro crea usuario en estado PENDIENTE ====================

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
        // RB-1: usuario tiene un único rol (null mientras está pendiente)
        assertNull(resultado.getRol());
        verify(usuarioRepository, times(2)).save(any(Usuario.class));
    }

    @Test
    void registrarUsuarioGuardaUnSoloRolCuandoEsAsignado() {
        // RB-1: un usuario tiene un único rol a la vez
        when(usuarioRepository.findByUsername("maria")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("encoded-maria");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(usuarioRepository.findById(10L)).thenReturn(Optional.empty());
        // El flujo normal de registro no asigna rol; solo lo hacemos vía aprobarUsuario.
        when(usuarioRepository.findById(10L)).thenAnswer(inv -> {
            // Construimos manualmente un usuario pendiente
            Usuario u = new Usuario();
            u.setId(10L);
            u.setUsername("maria");
            u.setEmail("maria@grupocordillera.com");
            u.setEstado(Usuario.Estado.PENDIENTE);
            u.setRol(null);
            return Optional.of(u);
        });

        RegistroRequest request = new RegistroRequest();
        request.setUsername("maria");
        request.setEmail("maria@grupocordillera.com");
        request.setPassword("pass123");
        authService.registrar(request);

        UsuarioAdminResponse aprobado = authService.aprobarUsuario(10L, "ADMIN_VENTAS", "SUPER_ADMIN");

        // Debe tener un único valor de rol, no varios
        assertEquals("ADMIN_VENTAS", aprobado.getRol());
    }

    // ==================== NFR-SEG-2: contraseña persistida NUNCA igual al plano ====================

    @Test
    void registrarNoPersistePasswordPlanoSinoHashBcrypt() {
        // Usamos un encoder real para confirmar el comportamiento
        PasswordEncoder realEncoder = new BCryptPasswordEncoder();
        String passPlano = "MiPassSegura123!";
        when(passwordEncoder.encode(passPlano)).thenAnswer(inv -> realEncoder.encode(inv.getArgument(0)));
        when(usuarioRepository.findByUsername("seguro")).thenReturn(Optional.empty());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        when(usuarioRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        RegistroRequest request = new RegistroRequest();
        request.setUsername("seguro");
        request.setEmail("seguro@grupocordillera.com");
        request.setPassword(passPlano);
        authService.registrar(request);

        Usuario persistido = captor.getAllValues().get(0);
        // NFR-SEG-2: el password persistido NO es igual al plano
        assertEquals(false, passPlano.equals(persistido.getPassword()));
        // Y además es un hash bcrypt válido
        assertEquals(true, realEncoder.matches(passPlano, persistido.getPassword()));
    }

    // ==================== RF-2.2: registro con email fuera del dominio ====================

    @Test
    void registrarUsuarioConCorreoNoCorporativoFalla() {
        RegistroRequest request = new RegistroRequest();
        request.setUsername("juan");
        request.setEmail("juan@gmail.com");
        request.setPassword("1234");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.registrar(request));

        assertEquals("Solo se permiten correos corporativos: @grupocordillera.com", ex.getMessage());
    }

    // ==================== RF-1.1: login válido retorna JWT con rol correcto ====================

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

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("admin", response.getUsername());
        assertEquals("admin@grupocordillera.com", response.getEmail());
        assertEquals("SUPER_ADMIN", response.getRol());
    }

    @Test
    void loginExitosoConRolAdminVentasDevuelveElRolCorrecto() {
        // RF-1.1: JWT contiene el rol correcto (probamos otro rol para confirmar unicidad - RB-1)
        Usuario usuario = new Usuario();
        usuario.setEmail("vendedor@grupocordillera.com");
        usuario.setUsername("vendedor");
        usuario.setPassword("encoded");
        usuario.setEstado(Usuario.Estado.APROBADO);
        usuario.setRol(Usuario.Rol.ADMIN_VENTAS);

        when(usuarioRepository.findByEmail("vendedor@grupocordillera.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "encoded")).thenReturn(true);
        when(jwtService.generarToken("vendedor@grupocordillera.com", "ADMIN_VENTAS")).thenReturn("jwt-ventas");

        LoginRequest request = new LoginRequest();
        request.setEmail("vendedor@grupocordillera.com");
        request.setPassword("1234");

        LoginResponse response = authService.login(request);

        assertEquals("ADMIN_VENTAS", response.getRol());
        assertEquals("jwt-ventas", response.getToken());
    }

    // ==================== RF-1.2 / RB-2: login rechazado si estado != APROBADO ====================

    @Test
    void loginRechazadoSiUsuarioEstaPendiente() {
        Usuario usuario = new Usuario();
        usuario.setEmail("pendiente@grupocordillera.com");
        usuario.setPassword("encoded");
        usuario.setEstado(Usuario.Estado.PENDIENTE);
        when(usuarioRepository.findByEmail("pendiente@grupocordillera.com")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest();
        request.setEmail("pendiente@grupocordillera.com");
        request.setPassword("cualquiera");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Tu cuenta esta pendiente de aprobación", ex.getMessage());
        // No se consulta el password ni se genera token
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void loginRechazadoSiUsuarioEstaRechazado() {
        Usuario usuario = new Usuario();
        usuario.setEmail("rechazado@grupocordillera.com");
        usuario.setPassword("encoded");
        usuario.setEstado(Usuario.Estado.RECHAZADO);
        when(usuarioRepository.findByEmail("rechazado@grupocordillera.com")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest();
        request.setEmail("rechazado@grupocordillera.com");
        request.setPassword("cualquiera");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Tu cuenta fue rechazada", ex.getMessage());
    }

    @Test
    void loginRechazadoSiUsuarioEstaBloqueado() {
        Usuario usuario = new Usuario();
        usuario.setEmail("bloqueado@grupocordillera.com");
        usuario.setPassword("encoded");
        usuario.setEstado(Usuario.Estado.BLOQUEADO);
        when(usuarioRepository.findByEmail("bloqueado@grupocordillera.com")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest();
        request.setEmail("bloqueado@grupocordillera.com");
        request.setPassword("cualquiera");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Tu cuenta fue bloqueada", ex.getMessage());
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

    // ==================== RF-3.1: listar usuarios pendientes / todos ====================

    @Test
    void listarPendientesSoloRetornaUsuariosPendientes() {
        Usuario pendiente = new Usuario();
        pendiente.setId(1L);
        pendiente.setUsername("p1");
        pendiente.setEmail("p1@grupocordillera.com");
        pendiente.setEstado(Usuario.Estado.PENDIENTE);
        Usuario aprobado = new Usuario();
        aprobado.setId(2L);
        aprobado.setUsername("a1");
        aprobado.setEmail("a1@grupocordillera.com");
        aprobado.setEstado(Usuario.Estado.APROBADO);
        aprobado.setRol(Usuario.Rol.EJECUTIVO);

        when(usuarioRepository.findByEstado(Usuario.Estado.PENDIENTE)).thenReturn(List.of(pendiente));

        List<UsuarioAdminResponse> resultado = authService.listarPendientes();

        assertEquals(1, resultado.size());
        assertEquals("PENDIENTE", resultado.get(0).getEstado());
        assertEquals("p1", resultado.get(0).getUsername());
    }

    @Test
    void listarTodoRetornaTodosLosUsuarios() {
        Usuario u1 = new Usuario();
        u1.setId(1L);
        u1.setUsername("u1");
        u1.setEmail("u1@grupocordillera.com");
        u1.setEstado(Usuario.Estado.APROBADO);
        u1.setRol(Usuario.Rol.SUPER_ADMIN);
        Usuario u2 = new Usuario();
        u2.setId(2L);
        u2.setUsername("u2");
        u2.setEmail("u2@grupocordillera.com");
        u2.setEstado(Usuario.Estado.BLOQUEADO);
        u2.setRol(Usuario.Rol.ANALISTA);

        when(usuarioRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UsuarioAdminResponse> resultado = authService.listarTodo();

        assertEquals(2, resultado.size());
        assertEquals("u1", resultado.get(0).getUsername());
        assertEquals("u2", resultado.get(1).getUsername());
    }

    // ==================== RF-2.3: admin aprueba/rechaza usuario pendiente y asigna rol ====================

    @Test
    void aprobarUsuarioPendienteAsignaRolYEstadoAprobado() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setUsername("nuevo");
        usuario.setEmail("nuevo@grupocordillera.com");
        usuario.setEstado(Usuario.Estado.PENDIENTE);
        usuario.setRol(null);

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioAdminResponse resultado = authService.aprobarUsuario(5L, "ADMIN_CLIENTES", "SUPER_ADMIN");

        assertEquals("APROBADO", resultado.getEstado());
        assertEquals("ADMIN_CLIENTES", resultado.getRol());
    }

    @Test
    void aprobarUsuarioConRolInvalidoLanzaError() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setEstado(Usuario.Estado.PENDIENTE);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.aprobarUsuario(5L, "ROL_ILEGAL", "SUPER_ADMIN"));

        assertEquals("Rol invalido", ex.getMessage());
    }

    @Test
    void aprobarUsuarioConActorSinPermisosLanzaError() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setEstado(Usuario.Estado.PENDIENTE);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));

        // ADMIN_VENTAS no puede aprobar con rol SUPER_ADMIN
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.aprobarUsuario(5L, "SUPER_ADMIN", "ADMIN_VENTAS"));

        assertEquals("No tienes permisos para gestionar roles", ex.getMessage());
    }

    @Test
    void rechazarUsuarioPendienteCambiaEstadoARechazado() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setUsername("candidato");
        usuario.setEstado(Usuario.Estado.PENDIENTE);
        usuario.setRol(Usuario.Rol.ADMIN_VENTAS);

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioAdminResponse resultado = authService.rechazarUsuario(5L, "ADMIN_USUARIOS");

        assertEquals("RECHAZADO", resultado.getEstado());
    }

    @Test
    void rechazarSuperAdminSoloPuedeHacerloOtroSuperAdmin() {
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setRol(Usuario.Rol.SUPER_ADMIN);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(usuario));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.rechazarUsuario(99L, "ADMIN_USUARIOS"));

        assertEquals("No tienes permisos para rechazar a un SUPER_ADMIN", ex.getMessage());
    }

    // ==================== RF-3.2: bloquear / desbloquear usuario ====================

    @Test
    void bloquearUsuarioAprobadoPoneEstadoBloqueado() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsername("trabajador");
        usuario.setEstado(Usuario.Estado.APROBADO);
        usuario.setRol(Usuario.Rol.EJECUTIVO);

        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioAdminResponse resultado = authService.bloquearUsuario(7L, "ADMIN_USUARIOS");

        assertEquals("BLOQUEADO", resultado.getEstado());
    }

    @Test
    void desbloquearUsuarioBloqueadoPoneEstadoAprobado() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsername("trabajador");
        usuario.setEstado(Usuario.Estado.BLOQUEADO);
        usuario.setRol(Usuario.Rol.EJECUTIVO);

        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioAdminResponse resultado = authService.desbloquearUsuario(7L, "ADMIN_USUARIOS");

        assertEquals("APROBADO", resultado.getEstado());
    }

    @Test
    void bloquearSuperAdminSoloPuedeHacerloOtroSuperAdmin() {
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setRol(Usuario.Rol.SUPER_ADMIN);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(usuario));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.bloquearUsuario(99L, "ADMIN_USUARIOS"));

        assertEquals("No tienes permisos para bloquear a un SUPER_ADMIN", ex.getMessage());
    }

    // ==================== NFR-SEG-9: JWT claims (rol + expiración) ====================

    @Test
    void loginLlamaAGenerarTokenConRolParaQueJwtIncluyaClaims() {
        // NFR-SEG-9: el JWT es generado con email y rol; la expiración es responsabilidad de JwtService,
        // pero aquí confirmamos que AuthService pasa los claims correctos.
        Usuario usuario = new Usuario();
        usuario.setEmail("analista@grupocordillera.com");
        usuario.setUsername("analista");
        usuario.setPassword("encoded");
        usuario.setEstado(Usuario.Estado.APROBADO);
        usuario.setRol(Usuario.Rol.ANALISTA);

        when(usuarioRepository.findByEmail("analista@grupocordillera.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "encoded")).thenReturn(true);
        when(jwtService.generarToken(eq("analista@grupocordillera.com"), eq("ANALISTA")))
                .thenReturn("token-analista");

        LoginRequest request = new LoginRequest();
        request.setEmail("analista@grupocordillera.com");
        request.setPassword("1234");

        LoginResponse response = authService.login(request);

        assertEquals("token-analista", response.getToken());
        assertEquals("ANALISTA", response.getRol());
    }
}
