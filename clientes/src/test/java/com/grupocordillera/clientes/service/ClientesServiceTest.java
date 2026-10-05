package com.grupocordillera.clientes.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.grupocordillera.clientes.dto.ClienteRequest;
import com.grupocordillera.clientes.dto.RegistrarCompraRequest;
import com.grupocordillera.clientes.model.Cliente;
import com.grupocordillera.clientes.repository.ClienteRepository;

@ExtendWith(MockitoExtension.class)
class ClientesServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClientesService clientesService;

    private ClienteRequest clienteRequest;

    @BeforeEach
    void setUp() {
        clienteRequest = new ClienteRequest();
        clienteRequest.setNombre("Juan");
        clienteRequest.setApellido("Perez");
        clienteRequest.setEmail("juan.perez@correo.com");
        clienteRequest.setTelefono("912345678");
        clienteRequest.setDireccion("Santiago");
        clienteRequest.setTipoCliente(null);
        clienteRequest.setEstado(null);
    }

    // ==================== RF-4.1: creación de cliente con campos válidos ====================

    @Test
    void crearClienteGuardaClienteNormalizado() {
        when(clienteRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente result = clientesService.crearCliente(clienteRequest);

        assertEquals("juan.perez@correo.com", result.getEmail());
        assertEquals(Cliente.TipoCliente.REGULAR, result.getTipoCliente());
        assertEquals(Cliente.Estado.ACTIVO, result.getEstado());
        assertEquals(0, result.getCantidadCompras());
        assertEquals(BigDecimal.ZERO, result.getMontoAcumulado());
        assertEquals(LocalDate.now(), result.getFechaRegistro());
        assertNull(result.getUltimaFechaCompra());
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void crearClienteConTipoYEstadoPersonalizadosLosMantiene() {
        clienteRequest.setTipoCliente(Cliente.TipoCliente.MAYORISTA);
        clienteRequest.setEstado(Cliente.Estado.INACTIVO);
        when(clienteRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente result = clientesService.crearCliente(clienteRequest);

        assertEquals(Cliente.TipoCliente.MAYORISTA, result.getTipoCliente());
        assertEquals(Cliente.Estado.INACTIVO, result.getEstado());
    }

    @Test
    void crearClienteLanzaErrorSiEmailYaExiste() {
        Cliente existente = new Cliente();
        existente.setId(1L);
        when(clienteRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(existente));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> clientesService.crearCliente(clienteRequest));

        assertEquals("Ya existe un cliente registrado con ese email", ex.getMessage());
        verify(clienteRepository, never()).save(any());
    }

    // ==================== NFR-SEG-5: datos de entrada maliciosos / malformados ====================

    @Test
    void crearClienteConStringInyeccionSqlServiceLoAceptaTalCual() {
        // NFR-SEG-5: @Valid está en el Controller. A nivel Service, estos caracteres
        // se persisten sin rechazo. Eso es correcto a nivel de capa Service; la
        // validación estructural la hace el Controller.
        // HALLAZGO: el Service NO sanitiza strings de entrada (SQL injection, XSS, etc.).
        // Se delega 100% en el Controller. Si alguien llama al Service directamente
        // bypassando el Controller, no hay protección en esta capa.
        clienteRequest.setNombre("'; DROP TABLE clientes; --");
        clienteRequest.setApellido("<script>alert('xss')</script>");
        clienteRequest.setDireccion("\" OR 1=1 --");
        when(clienteRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente result = clientesService.crearCliente(clienteRequest);

        // La capa Service no valida ni limpia: guarda el payload crudo.
        assertEquals("'; DROP TABLE clientes; --", result.getNombre());
        assertEquals("<script>alert('xss')</script>", result.getApellido());
        assertEquals("\" OR 1=1 --", result.getDireccion());
    }

    @Test
    void crearClienteConCamposVaciosServiceLosPersisteTalCual() {
        // NFR-SEG-5: strings vacíos o en blanco son aceptados por el Service.
        // Nuevamente, la validación @NotBlank / @Email vive en Controller.
        clienteRequest.setNombre("");
        clienteRequest.setApellido("   ");
        clienteRequest.setTelefono("");
        clienteRequest.setDireccion("");
        lenient().when(clienteRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente result = clientesService.crearCliente(clienteRequest);

        assertEquals("", result.getNombre());
        assertEquals("   ", result.getApellido());
        assertEquals("", result.getTelefono());
    }

    // ==================== RF-4.2: listar / consultar por tipo y por estado ====================

    @Test
    void obtenerTodosRetornaListaDelRepository() {
        Cliente c1 = new Cliente();
        c1.setId(1L);
        c1.setNombre("A");
        Cliente c2 = new Cliente();
        c2.setId(2L);
        c2.setNombre("B");
        when(clienteRepository.findAll()).thenReturn(List.of(c1, c2));

        List<Cliente> resultado = clientesService.obtenerTodos();

        assertEquals(2, resultado.size());
        assertEquals("A", resultado.get(0).getNombre());
    }

    @Test
    void obtenerPorTipoFiltraPorTipoCliente() {
        Cliente vip = new Cliente();
        vip.setId(1L);
        vip.setTipoCliente(Cliente.TipoCliente.VIP);
        when(clienteRepository.findByTipoCliente(Cliente.TipoCliente.VIP)).thenReturn(List.of(vip));

        List<Cliente> resultado = clientesService.obtenerPorTipo(Cliente.TipoCliente.VIP);

        assertEquals(1, resultado.size());
        assertEquals(Cliente.TipoCliente.VIP, resultado.get(0).getTipoCliente());
    }

    @Test
    void obtenerPorEstadoFiltraPorEstado() {
        Cliente inactivo = new Cliente();
        inactivo.setId(1L);
        inactivo.setEstado(Cliente.Estado.INACTIVO);
        when(clienteRepository.findByEstado(Cliente.Estado.INACTIVO)).thenReturn(List.of(inactivo));

        List<Cliente> resultado = clientesService.obtenerPorEstado(Cliente.Estado.INACTIVO);

        assertEquals(1, resultado.size());
        assertEquals(Cliente.Estado.INACTIVO, resultado.get(0).getEstado());
    }

    // ==================== Obtener por id con HALLAZGO ====================

    @Test
    void obtenerPorIdLanzaErrorSiNoExiste() {
        // HALLAZGO: El service lanza RuntimeException genérica. GlobalExceptionHandler
        // no mapea RuntimeException a 404 (lo convierte en 500). El requisito pide
        // 404 para id inexistente, pero el comportamiento ACTUAL retorna 500.
        // Documentamos el comportamiento REAL y no lo corregimos.
        when(clienteRepository.findById(eq(99L))).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> clientesService.obtenerPorId(99L));

        assertEquals("Cliente no encontrado con id: 99", ex.getMessage());
    }

    // ==================== RF-4.3: registrar compra actualiza totales y tipoCliente ====================

    @Test
    void registrarCompraActualizaTotales() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Juan");
        cliente.setApellido("Perez");
        cliente.setEmail("juan.perez@correo.com");
        cliente.setTelefono("912345678");
        cliente.setDireccion("Santiago");
        cliente.setTipoCliente(Cliente.TipoCliente.REGULAR);
        cliente.setEstado(Cliente.Estado.ACTIVO);
        cliente.setFechaRegistro(LocalDate.now().minusDays(10));
        cliente.setCantidadCompras(1);
        cliente.setMontoAcumulado(new BigDecimal("100000"));
        cliente.setUltimaFechaCompra(null);

        RegistrarCompraRequest request = new RegistrarCompraRequest();
        request.setMontoCompra(new BigDecimal("50000"));
        request.setFechaCompra(LocalDate.now());

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente result = clientesService.registrarCompra(1L, request);

        assertEquals(2, result.getCantidadCompras());
        assertEquals(new BigDecimal("150000"), result.getMontoAcumulado());
        assertEquals(LocalDate.now(), result.getUltimaFechaCompra());
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void registrarCompraSubeTipoRegularAFrecuentePorMontoAcumulado() {
        // RF-4.3: 3 compras o 500.000 -> FRECUENTE
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        cliente.setTipoCliente(Cliente.TipoCliente.REGULAR);
        cliente.setEstado(Cliente.Estado.ACTIVO);
        cliente.setFechaRegistro(LocalDate.now().minusDays(60));
        cliente.setCantidadCompras(1);
        cliente.setMontoAcumulado(new BigDecimal("490000"));
        cliente.setUltimaFechaCompra(LocalDate.now().minusDays(60));

        RegistrarCompraRequest request = new RegistrarCompraRequest();
        request.setMontoCompra(new BigDecimal("20000")); // total = 510.000 >= 500.000
        request.setFechaCompra(LocalDate.now());

        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente result = clientesService.registrarCompra(2L, request);

        assertEquals(Cliente.TipoCliente.FRECUENTE, result.getTipoCliente());
        assertEquals(new BigDecimal("510000"), result.getMontoAcumulado());
        assertEquals(2, result.getCantidadCompras());
    }

    @Test
    void registrarCompraSubeTipoAVIPCuandoCumpleCantidadYMonto() {
        // RF-4.3: >= 8 compras Y >= 1.500.000 -> VIP
        Cliente cliente = new Cliente();
        cliente.setId(3L);
        cliente.setTipoCliente(Cliente.TipoCliente.FRECUENTE);
        cliente.setEstado(Cliente.Estado.ACTIVO);
        cliente.setFechaRegistro(LocalDate.now().minusDays(120));
        cliente.setCantidadCompras(7);
        cliente.setMontoAcumulado(new BigDecimal("1490000"));
        cliente.setUltimaFechaCompra(LocalDate.now().minusDays(10));

        RegistrarCompraRequest request = new RegistrarCompraRequest();
        request.setMontoCompra(new BigDecimal("20000"));
        request.setFechaCompra(LocalDate.now());

        when(clienteRepository.findById(3L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente result = clientesService.registrarCompra(3L, request);

        assertEquals(Cliente.TipoCliente.VIP, result.getTipoCliente());
        assertEquals(8, result.getCantidadCompras());
        assertEquals(new BigDecimal("1510000"), result.getMontoAcumulado());
    }

    @Test
    void registrarCompraNoCambiaTipoManualMayorista() {
        // RF-4.3: tipos manuales (CORPORATIVO / MAYORISTA) no se recalculan.
        Cliente cliente = new Cliente();
        cliente.setId(4L);
        cliente.setTipoCliente(Cliente.TipoCliente.MAYORISTA);
        cliente.setEstado(Cliente.Estado.ACTIVO);
        cliente.setCantidadCompras(0);
        cliente.setMontoAcumulado(BigDecimal.ZERO);

        RegistrarCompraRequest request = new RegistrarCompraRequest();
        request.setMontoCompra(new BigDecimal("5000000"));
        request.setFechaCompra(LocalDate.now());

        when(clienteRepository.findById(4L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente result = clientesService.registrarCompra(4L, request);

        // Sigue siendo MAYORISTA a pesar del monto alto
        assertEquals(Cliente.TipoCliente.MAYORISTA, result.getTipoCliente());
    }

    @Test
    void registrarCompraRechazadaSiClienteNoEstaActivo() {
        Cliente cliente = new Cliente();
        cliente.setId(5L);
        cliente.setEstado(Cliente.Estado.INACTIVO);
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));

        RegistrarCompraRequest request = new RegistrarCompraRequest();
        request.setMontoCompra(BigDecimal.TEN);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> clientesService.registrarCompra(5L, request));

        assertEquals("El cliente no esta activo para registrar compras", ex.getMessage());
        verify(clienteRepository, never()).save(any());
    }
}
