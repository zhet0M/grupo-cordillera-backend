package com.grupocordillera.inventario.factory;

import com.grupocordillera.inventario.model.Producto;
import com.grupocordillera.inventario.model.ProductoHogar;
import com.grupocordillera.inventario.model.ProductoTecnologia;
import org.springframework.stereotype.Component;

@Component
public class ProductoFactory {

    public Producto crearProducto(String categoria) {
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría no puede ser nula");
        }

        return switch (categoria.toUpperCase()) {
            case "TECNOLOGIA" -> new ProductoTecnologia();
            case "HOGAR" -> new ProductoHogar();
            default -> throw new IllegalArgumentException("Categoría no soportada: " + categoria);
        };
    }
}
