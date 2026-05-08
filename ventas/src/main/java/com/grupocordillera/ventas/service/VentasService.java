package com.grupocordillera.ventas.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.grupocordillera.ventas.client.InventarioClient;
import com.grupocordillera.ventas.model.Venta;
import com.grupocordillera.ventas.repository.VentasRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VentasService {

    private static final int MAX_INTENTOS_FINANZAS = 5;

    private final VentasRepository ventasRepository;
    private final InventarioClient inventarioClient;
    private final FinanzasSyncService finanzasSyncService;

    public Venta registrarVenta(Venta venta) {
        Map<String, Object> producto = inventarioClient.porSku(venta.getSkuProducto());
        Long productoId = extraerLong(producto.get("id"));
        String nombreProducto = (String) producto.get("nombre");
        Double precioUnitario = extraerDouble(producto.get("precio"));

        if (productoId == null || nombreProducto == null || precioUnitario == null) {
            throw new RuntimeException("Respuesta invalida desde inventario al obtener el producto");
        }

        inventarioClient.descontarStock(venta.getSkuProducto(), venta.getCantidad());

        venta.setProductoId(productoId);
        venta.setNombreProducto(nombreProducto);
        venta.setPrecioUnitario(precioUnitario);
        venta.setMontoTotal(precioUnitario * venta.getCantidad());
        venta.setFecha(venta.getFecha() != null ? venta.getFecha() : LocalDate.now());
        venta.setEstadoFinanzas(Venta.EstadoFinanzas.PENDIENTE);
        venta.setIntentosFinanzas(0);
        venta.setUltimoErrorFinanzas(null);
        venta.setFechaUltimoIntentoFinanzas(null);

        Venta ventaGuardada = ventasRepository.save(venta);
        return finanzasSyncService.sincronizarVentaConFinanzas(ventaGuardada);
    }

    @Scheduled(fixedDelayString = "${finanzas.reintento.delay-ms:60000}")
    public void reprocesarVentasPendientesProgramado() {
        reprocesarVentasPendientes();
    }

    public int reprocesarVentasPendientes() {
        List<Venta> pendientes = obtenerVentasPendientesFinanzas();
        int reprocesadas = 0;

        for (Venta venta : pendientes) {
            if ((venta.getIntentosFinanzas() == null ? 0 : venta.getIntentosFinanzas()) >= MAX_INTENTOS_FINANZAS) {
                continue;
            }
            Venta resultado = finanzasSyncService.sincronizarVentaConFinanzas(venta);
            if (resultado.getEstadoFinanzas() == Venta.EstadoFinanzas.SINCRONIZADO) {
                reprocesadas++;
            }
        }

        return reprocesadas;
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

    public List<Venta> obtenerPorRango(LocalDate inicio, LocalDate fin) {
        return ventasRepository.findByFechaBetween(inicio, fin);
    }

    public List<Venta> obtenerHoy() {
        return ventasRepository.findByFecha(LocalDate.now());
    }

    public Double totalPorPeriodo(LocalDate inicio, LocalDate fin) {
        Double total = ventasRepository.sumMontoTotalByFechaBetween(inicio, fin);
        return total != null ? total : 0.0;
    }

    public Double totalPorSucursal(String sucursal) {
        Double total = ventasRepository.sumMontoTotalBySucursal(sucursal);
        return total != null ? total : 0.0;
    }

    public List<Venta> obtenerVentasPendientesFinanzas() {
        return ventasRepository.findByEstadoFinanzasIn(List.of(Venta.EstadoFinanzas.PENDIENTE, Venta.EstadoFinanzas.ERROR));
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

}
