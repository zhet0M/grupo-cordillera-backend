package com.grupocordillera.inventario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
}
