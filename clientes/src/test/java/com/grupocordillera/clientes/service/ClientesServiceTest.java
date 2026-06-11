package com.grupocordillera.clientes.service;

import com.grupocordillera.clientes.dto.ClienteRequest;
import com.grupocordillera.clientes.dto.RegistrarCompraRequest;
import com.grupocordillera.clientes.model.Cliente;
import com.grupocordillera.clientes.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        verify(clienteRepository).save(any(Cliente.class));
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
    void obtenerPorIdLanzaErrorSiNoExiste() {
        when(clienteRepository.findById(eq(99L))).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> clientesService.obtenerPorId(99L));

        assertEquals("Cliente no encontrado con id: 99", ex.getMessage());
    }
}
