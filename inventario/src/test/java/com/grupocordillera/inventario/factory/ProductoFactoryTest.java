package com.grupocordillera.inventario.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.grupocordillera.inventario.model.Producto;
import com.grupocordillera.inventario.model.ProductoHogar;
import com.grupocordillera.inventario.model.ProductoTecnologia;

@ExtendWith(MockitoExtension.class)
class ProductoFactoryTest {

    @InjectMocks
    private ProductoFactory productoFactory;

    // ==================== RF-5.5: Factory Method crea instancia correcta ====================

    @Test
    void crearProductoTecnologiaRetornaInstanciaProductoTecnologia() {
        Producto resultado = productoFactory.crearProducto("TECNOLOGIA");

        assertInstanceOf(ProductoTecnologia.class, resultado);
        //HALLAZGO: la factory solo instancia new ProductoTecnologia() NO setea
        // la propiedad `categoria` internamente. Quien debe setearla es quien invoca
        // al factory (InventarioService.registrarProducto ya lo hace antes de persistir).
        assertNull(((ProductoTecnologia) resultado).getCategoria());
    }

    @Test
    void crearProductoHogarRetornaInstanciaProductoHogar() {
        Producto resultado = productoFactory.crearProducto("HOGAR");

        assertInstanceOf(ProductoHogar.class, resultado);
        //HALLAZGO: idem tecnología — ProductoHogar.categoria NO es inicializada
        // por la factory. Sigue en null hasta que InventarioService lo setee.
        assertNull(((ProductoHogar) resultado).getCategoria());
    }

    @Test
    void crearProductoConCategoriaEnMinusculasFuncionaPorElToUpperCase() {
        Producto tec = productoFactory.crearProducto("tecnologia");
        Producto hog = productoFactory.crearProducto("hogar");

        assertInstanceOf(ProductoTecnologia.class, tec);
        assertInstanceOf(ProductoHogar.class, hog);
    }

    @Test
    void crearProductoConCategoriaConEspaciosAlrededorLanzaExcepcion() {
        //HALLAZGO: la factory NO invoca .trim() antes del switch. Inputs con
        // espacios alrededor ("  TECNOLOGIA  ", "  hogar\t") son tratados como
        // categorías NO válidas y lanzan IllegalArgumentException.
        // El input debe venir ya normalizado (InventarioService.registrarProducto
        // sí hace el trim ANTES de llamar a este factory).
        assertThrows(IllegalArgumentException.class,
                () -> productoFactory.crearProducto("  TECNOLOGIA  "));

        assertThrows(IllegalArgumentException.class,
                () -> productoFactory.crearProducto("  hogar\t"));
    }

    @Test
    void crearProductoConCategoriaNulaLanzaErrorConMensajeCorrecto() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoFactory.crearProducto(null));

        assertEquals("La categoría no puede ser nula", ex.getMessage());
    }

    @Test
    void crearProductoConCategoriaInvalidaLanzaErrorConMensajeIncluyeLaCategoria() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoFactory.crearProducto("COMIDA"));

        // El mensaje incluye la categoría ORIGINAL (la del usuario), no la normalizada
        assertEquals("Categoría no soportada: COMIDA", ex.getMessage());
    }
}
