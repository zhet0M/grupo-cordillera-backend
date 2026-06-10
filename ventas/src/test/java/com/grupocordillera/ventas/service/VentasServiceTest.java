package com.grupocordillera.ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.grupocordillera.ventas.client.ClientesClient;
import com.grupocordillera.ventas.client.InventarioClient;
import com.grupocordillera.ventas.dto.ClienteDTO;
import com.grupocordillera.ventas.model.Venta;
import com.grupocordillera.ventas.repository.VentasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VentasServiceTest {

    @Mock
    private VentasRepository ventasRepository;

    @Mock
    private InventarioClient inventarioClient;

    @Mock
    private ClientesClient clientesClient;

    @Mock
    private FinanzasSyncService finanzasSyncService;

    @InjectMocks
    private VentasService ventasService;

    private final AtomicReference<Venta> savedVenta = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        lenient().when(ventasRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta venta = invocation.getArgument(0);
            if (venta.getId() == null) {
                venta.setId(1L);
            }
            savedVenta.set(venta);
            return venta;
        });
        lenient().when(ventasRepository.findById(1L)).thenAnswer(invocation -> Optional.ofNullable(savedVenta.get()));
        lenient().when(finanzasSyncService.sincronizarVentaConFinanzas(any(Venta.class))).thenAnswer(invocation -> {
            Venta venta = invocation.getArgument(0);
            venta.setEstadoFinanzas(Venta.EstadoFinanzas.SINCRONIZADO);
            return venta;
        });
    }

    @Test
    void registrarVentaPosSinClienteSincronizaStockYFinanzas() {
        when(inventarioClient.porSku("TEC-001")).thenReturn(productoInventario(7L, "Notebook", 500.0, "SUC-CENTRAL"));

        Venta venta = ventaBase(Venta.Canal.POS, "SUC-CENTRAL", "TEC-001", null);
        venta.setCantidad(2);

        Venta resultado = ventasService.registrarVenta(venta);

        assertEquals(1000.0, resultado.getMontoTotal());
        assertEquals(Venta.EstadoFinanzas.SINCRONIZADO, resultado.getEstadoFinanzas());
        assertEquals(7L, resultado.getProductoId());
        assertEquals("Notebook", resultado.getNombreProducto());
        assertEquals(500.0, resultado.getPrecioUnitario());
        verify(inventarioClient).descontarStock("TEC-001", 2);
        verifyNoInteractions(clientesClient);
    }

    @Test
    void registrarVentaEcommerceRegistraClienteYActualizaSnapshot() {
        when(inventarioClient.porSku("TEC-002")).thenReturn(productoInventario(8L, "Monitor", 250.0, "SUC-NORTE"));

        ClienteDTO cliente = new ClienteDTO();
        cliente.setId(55L);
        cliente.setNombre("Ana");
        cliente.setApellido("Perez");
        cliente.setEmail("ana@grupocordillera.com");
        cliente.setTelefono("123");
        cliente.setDireccion("Calle 1");
        cliente.setTipoCliente("REGULAR");
        cliente.setEstado("ACTIVO");

        when(clientesClient.obtenerPorId(55L)).thenReturn(cliente);
        when(clientesClient.registrarCompra(eq(55L), any())).thenReturn(cliente);

        Venta venta = ventaBase(Venta.Canal.ECOMMERCE, "SUC-NORTE", "TEC-002", 55L);
        venta.setCantidad(1);

        Venta resultado = ventasService.registrarVenta(venta);

        assertEquals(55L, resultado.getClienteId());
        assertEquals("Ana", resultado.getNombreCliente());
        assertEquals("Perez", resultado.getApellidoCliente());
        assertEquals(Venta.EstadoFinanzas.SINCRONIZADO, resultado.getEstadoFinanzas());
        verify(clientesClient).obtenerPorId(55L);
        verify(clientesClient).registrarCompra(eq(55L), any());
    }

    @Test
    void registrarVentaRechazaProductoDeOtraSucursal() {
        when(inventarioClient.porSku("TEC-003")).thenReturn(productoInventario(9L, "Teclado", 80.0, "SUC-ESTE"));

        Venta venta = ventaBase(Venta.Canal.POS, "SUC-CENTRAL", "TEC-003", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ventasService.registrarVenta(venta));

        assertEquals("El producto SKU TEC-003 pertenece a la sucursal SUC-ESTE, no a SUC-CENTRAL", ex.getMessage());
        verify(inventarioClient, never()).descontarStock(any(), anyInt());
        verifyNoInteractions(clientesClient, ventasRepository, finanzasSyncService);
    }

    private Venta ventaBase(Venta.Canal canal, String sucursal, String sku, Long clienteId) {
        Venta venta = new Venta();
        venta.setCanal(canal);
        venta.setSucursal(sucursal);
        venta.setSkuProducto(sku);
        venta.setClienteId(clienteId);
        venta.setFecha(LocalDate.now());
        venta.setCantidad(1);
        return venta;
    }

    private Map<String, Object> productoInventario(Long id, String nombre, Double precio, String sucursal) {
        Map<String, Object> producto = new HashMap<>();
        producto.put("id", id);
        producto.put("nombre", nombre);
        producto.put("precio", precio);
        producto.put("sucursal", sucursal);
        return producto;
    }
}
