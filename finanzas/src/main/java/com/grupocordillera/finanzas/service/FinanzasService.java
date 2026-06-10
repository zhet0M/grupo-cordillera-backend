package com.grupocordillera.finanzas.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.grupocordillera.finanzas.client.InventarioClient;
import com.grupocordillera.finanzas.dto.MovimientoFinancieroRequest;
import com.grupocordillera.finanzas.dto.ProductoInventarioDTO;
import com.grupocordillera.finanzas.dto.ResumenFinancieroDTO;
import com.grupocordillera.finanzas.model.MovimientoFinanciero;
import com.grupocordillera.finanzas.model.Sucursal;
import com.grupocordillera.finanzas.repository.MovimientoFinancieroRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinanzasService {

    private final MovimientoFinancieroRepository repository;
    private final InventarioClient inventarioClient;

    public MovimientoFinanciero registrarMovimiento(MovimientoFinancieroRequest request) {
        if (request.getVentaId() != null) {
            return repository.findByVentaId(request.getVentaId())
                    .orElseGet(() -> crearMovimiento(request));
        }

        return crearMovimiento(request);
    }

    private MovimientoFinanciero crearMovimiento(MovimientoFinancieroRequest request) {
        ProductoInventarioDTO producto = obtenerProductoExistente(request.getSkuProducto());
        validarConsistencia(request, producto);
        Sucursal sucursal = Sucursal.from(request.getSucursal());

        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setVentaId(request.getVentaId());
        movimiento.setProductoId(producto.getId());
        movimiento.setSkuProducto(producto.getSku());
        movimiento.setNombreProducto(producto.getNombre());
        movimiento.setCantidad(request.getCantidad() != null ? request.getCantidad() : 1);
        movimiento.setIngresos(valorSeguro(request.getIngresos()));
        movimiento.setCosto(obtenerCosto(request, producto));
        movimiento.setMargen(movimiento.getIngresos().subtract(movimiento.getCosto()));
        movimiento.setFechaRegistro(request.getFechaRegistro() != null ? request.getFechaRegistro() : LocalDate.now());
        movimiento.setSucursal(producto.getSucursal() != null ? Sucursal.from(producto.getSucursal()).valor() : sucursal.valor());
        movimiento.setTipoMovimiento(request.getTipoMovimiento());
        return repository.save(movimiento);
    }

    public List<MovimientoFinanciero> obtenerTodos() {
        return repository.findAll();
    }

    public MovimientoFinanciero obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movimiento financiero no encontrado con id: " + id));
    }

    public List<MovimientoFinanciero> obtenerPorFecha(LocalDate fecha) {
        return repository.findByFechaRegistro(fecha);
    }

    public List<MovimientoFinanciero> obtenerPorSucursal(String sucursal) {
        return repository.findBySucursalIgnoreCase(Sucursal.from(sucursal).valor());
    }

    public List<MovimientoFinanciero> obtenerPorTipo(MovimientoFinanciero.TipoMovimiento tipoMovimiento) {
        return repository.findByTipoMovimiento(tipoMovimiento);
    }

    public List<MovimientoFinanciero> obtenerPorRango(LocalDate inicio, LocalDate fin) {
        return repository.findByFechaRegistroBetween(inicio, fin);
    }

    public ResumenFinancieroDTO obtenerTotalesGenerales() {
        return new ResumenFinancieroDTO(
                repository.sumIngresos(),
                repository.sumCostos(),
                repository.sumMargen()
        );
    }

    public ResumenFinancieroDTO obtenerTotalesPorRango(LocalDate inicio, LocalDate fin) {
        return new ResumenFinancieroDTO(
                repository.sumIngresosByFechaRegistroBetween(inicio, fin),
                repository.sumCostosByFechaRegistroBetween(inicio, fin),
                repository.sumMargenByFechaRegistroBetween(inicio, fin)
        );
    }

    public ResumenFinancieroDTO obtenerTotalesPorSucursal(String sucursal) {
        String sucursalNormalizada = Sucursal.from(sucursal).valor();
        return new ResumenFinancieroDTO(
                repository.sumIngresosBySucursal(sucursalNormalizada),
                repository.sumCostosBySucursal(sucursalNormalizada),
                repository.sumMargenBySucursal(sucursalNormalizada)
        );
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private ProductoInventarioDTO obtenerProductoExistente(String skuProducto) {
        try {
            ProductoInventarioDTO producto = inventarioClient.obtenerPorSku(skuProducto);
            if (producto == null || producto.getId() == null) {
                throw new RuntimeException("Producto no encontrado en inventario para el SKU: " + skuProducto);
            }
            return producto;
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo validar el producto en inventario. SKU: " + skuProducto);
        }
    }

    private void validarConsistencia(MovimientoFinancieroRequest request, ProductoInventarioDTO producto) {
        if (request.getProductoId() != null && !request.getProductoId().equals(producto.getId())) {
            throw new RuntimeException("El productoId no coincide con el producto real del inventario");
        }

        if (request.getNombreProducto() != null && !request.getNombreProducto().isBlank()
                && !request.getNombreProducto().equalsIgnoreCase(producto.getNombre())) {
            throw new RuntimeException("El nombre del producto no coincide con el inventario");
        }

        if (request.getSucursal() != null && producto.getSucursal() != null
                && !request.getSucursal().equalsIgnoreCase(producto.getSucursal())) {
            throw new RuntimeException("La sucursal enviada no coincide con la sucursal del producto en inventario");
        }
    }

    private BigDecimal obtenerCosto(MovimientoFinancieroRequest request, ProductoInventarioDTO producto) {
        if (request.getCosto() != null) {
            return request.getCosto();
        }

        if (producto.getCosto() == null) {
            throw new RuntimeException("El producto en inventario no tiene costo registrado");
        }

        return BigDecimal.valueOf(producto.getCosto()).multiply(BigDecimal.valueOf(request.getCantidad()));
    }
}
