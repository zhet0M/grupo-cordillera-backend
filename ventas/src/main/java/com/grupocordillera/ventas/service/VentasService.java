package com.grupocordillera.ventas.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.grupocordillera.ventas.client.InventarioClient;
import com.grupocordillera.ventas.model.Venta;
import com.grupocordillera.ventas.repository.VentasRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VentasService {

    private final VentasRepository ventasRepository;
    private final InventarioClient inventarioClient;

    @CircuitBreaker(name = "ventas", fallbackMethod = "fallbackRegistrarVenta")
    public Venta registrarVenta(Venta venta) {
        // Obtener datos del producto desde inventario
        Map<String, Object> producto = inventarioClient.porSku(venta.getSkuProducto());
        Long productoId = extraerLong(producto.get("id"));
        String nombreProducto = (String) producto.get("nombre");
        Double precioUnitario = extraerDouble(producto.get("precio"));

        if (productoId == null || nombreProducto == null || precioUnitario == null) {
            throw new RuntimeException("Respuesta invalida desde inventario al obtener el producto");
        }

        // Descontar stock en inventario
        inventarioClient.descontarStock(venta.getSkuProducto(), venta.getCantidad());
        
        // Poblar los datos de la venta con la información del producto
        venta.setProductoId(productoId);
        venta.setNombreProducto(nombreProducto);
        venta.setPrecioUnitario(precioUnitario);
        venta.setMontoTotal(precioUnitario * venta.getCantidad());
        
        if (venta.getFecha() == null) {
            venta.setFecha(LocalDate.now());
        }
        
        // Guardar la venta
        return ventasRepository.save(venta);
    }

    public Venta fallbackRegistrarVenta(Venta venta, Throwable t) {
        if (esErrorBaseDatos(t)) {
            throw new RuntimeException("Error al guardar la venta en la base de datos. Detalle: " + extraerMensaje(t));
        }

        // Fallback Si el inventario falla (está caído, muy lento, o falla el stock)
        throw new RuntimeException("Circuit Breaker Abierto: No se pudo registrar la venta. Servicio de inventario no disponible o falló. Detalle: " + extraerMensaje(t));
    }

    public List<Venta> obtenerTodas() {
        return ventasRepository.findAll();
    }

    public List<Venta> obtenerPorFecha(LocalDate fecha) {
        return ventasRepository.findByFecha(fecha);
    }

    public List<Venta> obtenerPorSucursal(String sucursal) {
        return ventasRepository.findBySucursal(sucursal);
    }

    // Obtener ventas por rango de fechas
    public List<Venta> obtenerPorRango(LocalDate inicio, LocalDate fin) {
        return ventasRepository.findByFechaBetween(inicio, fin);
    }

    public List<Venta> obtenerHoy() {
        return ventasRepository.findByFecha(LocalDate.now());
    }

    // Total de ventas por período
    public Double totalPorPeriodo(LocalDate inicio, LocalDate fin) {
        Double total = ventasRepository.sumMontoTotalByFechaBetween(inicio, fin);
        return total != null ? total : 0.0;
    }

    public Double totalPorSucursal(String sucursal) {
        Double total = ventasRepository.sumMontoTotalBySucursal(sucursal);
        return total != null ? total : 0.0;
    }

    public void eliminarVenta(Long id) {
        ventasRepository.deleteById(id);
    }

    private Long extraerLong(Object valor) {
        if (valor instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private Double extraerDouble(Object valor) {
        if (valor instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private boolean esErrorBaseDatos(Throwable t) {
        Throwable actual = t;
        while (actual != null) {
            if (actual instanceof DataAccessException || actual instanceof PersistenceException) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    private String extraerMensaje(Throwable t) {
        Throwable actual = t;
        String ultimoMensaje = null;

        while (actual != null) {
            if (actual.getMessage() != null && !actual.getMessage().isBlank()) {
                ultimoMensaje = actual.getMessage();
            }
            actual = actual.getCause();
        }

        return ultimoMensaje != null ? ultimoMensaje : "sin detalle";
    }
}
