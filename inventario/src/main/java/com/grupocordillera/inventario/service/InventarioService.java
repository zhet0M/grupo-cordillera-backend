package com.grupocordillera.inventario.service;

import com.grupocordillera.inventario.model.Producto;
import com.grupocordillera.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final ProductoRepository productoRepository;

    public Producto registrarProducto(Producto producto) {
        producto.setFechaIngreso(producto.getFechaIngreso() != null ? producto.getFechaIngreso() : LocalDate.now());
        actualizarEstado(producto);
        
        return productoRepository.save(producto);
    }

    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    public Producto obtenerPorSku(String sku) {
        return productoRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con SKU: " + sku));
    }

    public List<Producto> obtenerPorSucursal(String sucursal) {
        return productoRepository.findBySucursal(sucursal);
    }

    // Método principal para cuando ocurre una venta
    public Producto descontarStock(String sku, int cantidad) {
        Producto producto = obtenerPorSku(sku);
        if (producto.getStock() < cantidad) {
            throw new RuntimeException("Stock insuficiente para el producto: " + sku);
        }
        producto.setStock(producto.getStock() - cantidad);
        actualizarEstado(producto);
        return productoRepository.save(producto);
    }

    private void actualizarEstado(Producto producto) {
        if (producto.getEstado() == Producto.Estado.DESCONTINUADO) {
            return;
        }
        if (producto.getStock() <= 0) {
            producto.setEstado(Producto.Estado.AGOTADO);
        } else {
            producto.setEstado(Producto.Estado.DISPONIBLE);
        }
    }
}
