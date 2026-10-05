package com.grupocordillera.inventario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.grupocordillera.inventario.model.Producto;
import com.grupocordillera.inventario.model.ProductoHogar;
import com.grupocordillera.inventario.model.ProductoTecnologia;
import com.grupocordillera.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private InventarioService inventarioService;

    @BeforeEach
    void setUp() {
        lenient().when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registrarProductoTecnologiaGeneraSkuSecuencialYNormalizaSucursal() {
        when(productoRepository.findBySkuStartingWith("TEC-")).thenReturn(List.of(productoTecnologia("TEC-001"), productoTecnologia("TEC-002")));

        ProductoTecnologia producto = new ProductoTecnologia();
        producto.setCategoria("TECNOLOGIA");
        producto.setNombre("Notebook");
        producto.setPrecio(500.0);
        producto.setCosto(300.0);
        producto.setStock(10);
        producto.setStockMinimo(2);
        producto.setSucursal("suc-central");

        Producto resultado = inventarioService.registrarProducto(producto);

        assertEquals("TEC-003", resultado.getSku());
        assertEquals("SUC-CENTRAL", resultado.getSucursal());
        assertEquals(LocalDate.now(), resultado.getFechaIngreso());
    }

    @Test
    void registrarProductoHogarGeneraPrimerSkuDisponible() {
        when(productoRepository.findBySkuStartingWith("HOG-")).thenReturn(List.of());

        ProductoHogar producto = new ProductoHogar();
        producto.setCategoria("HOGAR");
        producto.setNombre("Mesa");
        producto.setPrecio(120.0);
        producto.setCosto(60.0);
        producto.setStock(5);
        producto.setStockMinimo(1);
        producto.setSucursal("SUC-NORTE");

        Producto resultado = inventarioService.registrarProducto(producto);

        assertEquals("HOG-001", resultado.getSku());
        assertEquals("SUC-NORTE", resultado.getSucursal());
    }

    @Test
    void obtenerPorSkuNormalizaMayusculas() {
        ProductoTecnologia producto = productoTecnologia("TEC-010");
        when(productoRepository.findBySku("TEC-010")).thenReturn(Optional.of(producto));

        Producto resultado = inventarioService.obtenerPorSku("tec-010");

        assertSame(producto, resultado);
        verify(productoRepository).findBySku("TEC-010");
    }

    @Test
    void obtenerPorSucursalNormalizaSucursal() {
        ProductoTecnologia producto = productoTecnologia("TEC-001");
        when(productoRepository.findBySucursal("SUC-OESTE")).thenReturn(List.of(producto));

        List<Producto> resultado = inventarioService.obtenerPorSucursal("suc-oeste");

        assertEquals(1, resultado.size());
        assertSame(producto, resultado.get(0));
        verify(productoRepository).findBySucursal("SUC-OESTE");
    }

    private ProductoTecnologia productoTecnologia(String sku) {
        ProductoTecnologia producto = new ProductoTecnologia();
        producto.setSku(sku);
        producto.setCategoria("TECNOLOGIA");
        producto.setNombre("Producto");
        producto.setPrecio(1.0);
        producto.setCosto(1.0);
        producto.setStock(1);
        producto.setStockMinimo(1);
        producto.setSucursal("SUC-CENTRAL");
        return producto;
    }

    // ==================== Descuento de stock PUT /inventario/descontar/{sku} ====================

    @Test
    void descontarStockConStockSuficienteActualizaStockYGuarda() {
        ProductoTecnologia producto = productoTecnologia("TEC-007");
        producto.setId(7L);
        producto.setStock(10);
        producto.setStockMinimo(3);
        producto.setEstado(Producto.Estado.DISPONIBLE);

        when(productoRepository.findBySku("TEC-007")).thenReturn(java.util.Optional.of(producto));

        com.grupocordillera.inventario.model.Producto resultado = inventarioService.descontarStock("TEC-007", 3);

        assertEquals(7, resultado.getStock());
        assertEquals(Producto.Estado.DISPONIBLE, resultado.getEstado());
        verify(productoRepository).save(producto);
    }

    @Test
    void descontarStockHastaCeroPoneEstadoAgotado() {
        ProductoTecnologia producto = productoTecnologia("TEC-008");
        producto.setId(8L);
        producto.setStock(2);
        producto.setStockMinimo(1);
        producto.setEstado(Producto.Estado.DISPONIBLE);

        when(productoRepository.findBySku("TEC-008")).thenReturn(java.util.Optional.of(producto));

        com.grupocordillera.inventario.model.Producto resultado = inventarioService.descontarStock("TEC-008", 2);

        assertEquals(0, resultado.getStock());
        assertEquals(Producto.Estado.AGOTADO, resultado.getEstado());
    }

    @Test
    void descontarStockInsuficienteLanzaRuntimeException() {
        ProductoTecnologia producto = productoTecnologia("TEC-009");
        producto.setStock(1);
        producto.setStockMinimo(1);

        when(productoRepository.findBySku("TEC-009")).thenReturn(java.util.Optional.of(producto));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> inventarioService.descontarStock("TEC-009", 5));

        assertEquals("Stock insuficiente para el producto: TEC-009", ex.getMessage());
        // Stock no cambia y no se guarda
        assertEquals(1, producto.getStock());
        verify(productoRepository, never()).save(any());
    }

    @Test
    void descontarStockConSkuMinusculasNormalizaAMayusculas() {
        ProductoTecnologia producto = productoTecnologia("TEC-015");
        producto.setId(15L);
        producto.setStock(20);
        producto.setEstado(Producto.Estado.DISPONIBLE);

        when(productoRepository.findBySku("TEC-015")).thenReturn(java.util.Optional.of(producto));

        inventarioService.descontarStock("tec-015", 5);

        assertEquals(15, producto.getStock());
        verify(productoRepository).findBySku("TEC-015");
    }

    @Test
    void registrarProductoConCategoriaInvalidaLanzaErrorAlGenerarSku() {
        com.grupocordillera.inventario.model.ProductoHogar producto = new com.grupocordillera.inventario.model.ProductoHogar();
        producto.setCategoria("CATEGORIA_RARA");
        producto.setNombre("Raro");
        producto.setPrecio(10.0);
        producto.setCosto(5.0);
        producto.setStock(1);
        producto.setStockMinimo(1);
        producto.setSucursal("SUC-CENTRAL");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> inventarioService.registrarProducto(producto));

        assertEquals("Categoria no valida para generar SKU: CATEGORIA_RARA", ex.getMessage());
    }
}
