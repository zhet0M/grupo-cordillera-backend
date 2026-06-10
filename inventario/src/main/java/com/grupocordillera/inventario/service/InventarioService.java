package com.grupocordillera.inventario.service;

import com.grupocordillera.inventario.model.Producto;
import com.grupocordillera.inventario.model.Sucursal;
import com.grupocordillera.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private static final Pattern SKU_PATTERN = Pattern.compile("^(TEC|HOG)-(\\d+)$");

    private final ProductoRepository productoRepository;

    public Producto registrarProducto(Producto producto) {
        producto.setSucursal(Sucursal.from(producto.getSucursal()).valor());
        producto.setFechaIngreso(producto.getFechaIngreso() != null ? producto.getFechaIngreso() : LocalDate.now());

        if (producto.getId() == null) {
            producto.setSku(generarSku(producto.getCategoria()));
        } else {
            producto.setSku(normalizarSku(producto.getSku()));
            validarPrefijoSku(producto);
        }

        actualizarEstado(producto);
        
        return productoRepository.save(producto);
    }

    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    public Producto obtenerPorSku(String sku) {
        return productoRepository.findBySku(normalizarSku(sku))
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con SKU: " + sku));
    }

    public List<Producto> obtenerPorSucursal(String sucursal) {
        return productoRepository.findBySucursal(Sucursal.from(sucursal).valor());
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

    private String normalizarSku(String sku) {
        return sku == null ? null : sku.trim().toUpperCase(Locale.ROOT);
    }

    private void validarPrefijoSku(Producto producto) {
        String prefijoEsperado = "TECNOLOGIA".equalsIgnoreCase(producto.getCategoria()) ? "TEC-" : "HOG-";
        if (producto.getSku() == null || !producto.getSku().startsWith(prefijoEsperado)) {
            throw new IllegalArgumentException(
                    "El SKU debe comenzar con " + prefijoEsperado + " para la categoria " + producto.getCategoria());
        }
    }

    private String generarSku(String categoria) {
        String prefijo = "TECNOLOGIA".equalsIgnoreCase(categoria)
                ? "TEC-"
                : "HOGAR".equalsIgnoreCase(categoria)
                ? "HOG-"
                : null;

        if (prefijo == null) {
            throw new IllegalArgumentException("Categoria no valida para generar SKU: " + categoria);
        }

        int siguiente = productoRepository.findBySkuStartingWith(prefijo).stream()
                .map(Producto::getSku)
                .map(SKU_PATTERN::matcher)
                .filter(Matcher::matches)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(2)))
                .max()
                .orElse(0) + 1;

        return prefijo + String.format("%03d", siguiente);
    }
}
