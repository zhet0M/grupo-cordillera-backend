package com.grupocordillera.alertas.service;

import com.grupocordillera.alertas.client.FinanzasClient;
import com.grupocordillera.alertas.client.InventarioClient;
import com.grupocordillera.alertas.client.VentasClient;
import com.grupocordillera.alertas.dto.MovimientoFinancieroFuenteDTO;
import com.grupocordillera.alertas.dto.ProductoInventarioFuenteDTO;
import com.grupocordillera.alertas.dto.VentaFuenteDTO;
import com.grupocordillera.alertas.model.Alerta;
import com.grupocordillera.alertas.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeteccionAlertasService {

    private final AlertaRepository alertaRepository;
    private final InventarioClient inventarioClient;
    private final VentasClient ventasClient;
    private final FinanzasClient finanzasClient;

    @Scheduled(fixedDelayString = "${alertas.deteccion.delay-ms:60000}")
    public void detectarAutomaticamente() {
        try {
            detectarStockCritico();
            detectarVentasBajas();
            detectarMargenNormalizado();
        } catch (Exception ignored) {
            // Si una fuente falla, el servicio sigue vivo y reintenta en el siguiente ciclo.
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void detectarAlArranque() {
        detectarAutomaticamente();
    }

    public void detectarAhora() {
        detectarAutomaticamente();
    }

    private void detectarStockCritico() {
        List<ProductoInventarioFuenteDTO> productos = inventarioClient.obtenerProductos();
        for (ProductoInventarioFuenteDTO producto : productos) {
            if (producto.getSku() == null || producto.getNombre() == null) {
                continue;
            }
            boolean critico = producto.getStock() != null && producto.getStockMinimo() != null
                    && producto.getStock() <= producto.getStockMinimo()
                    && (producto.getEstado() == null || !"DESCONTINUADO".equalsIgnoreCase(producto.getEstado()));
            registrarSiCorresponde(
                    "STOCK_CRITICO:" + producto.getSku(),
                    Alerta.TipoAlerta.STOCK_CRITICO,
                    "Stock crítico",
                    producto.getNombre(),
                    critico
            );
        }
    }

    private void detectarVentasBajas() {
        List<VentaFuenteDTO> ventas = ventasClient.obtenerVentas();
        LocalDate hoy = LocalDate.now();
        LocalDate inicioActual = hoy.minusDays(6);
        LocalDate inicioAnterior = hoy.minusDays(13);
        LocalDate finAnterior = hoy.minusDays(7);

        Map<String, BigDecimal> ventasActuales = sumarPorSucursal(ventas, inicioActual, hoy);
        Map<String, BigDecimal> ventasPrevias = sumarPorSucursal(ventas, inicioAnterior, finAnterior);

        for (String sucursal : ventasActuales.keySet()) {
            BigDecimal actual = ventasActuales.getOrDefault(sucursal, BigDecimal.ZERO);
            BigDecimal anterior = ventasPrevias.getOrDefault(sucursal, BigDecimal.ZERO);

            boolean bajas = anterior.compareTo(BigDecimal.ZERO) > 0
                    ? actual.compareTo(anterior.multiply(BigDecimal.valueOf(0.75))) < 0
                    : actual.compareTo(BigDecimal.ZERO) == 0;

            registrarSiCorresponde(
                    "VENTAS_BAJAS:" + sucursal.toUpperCase(),
                    Alerta.TipoAlerta.VENTAS_BAJAS,
                    "Ventas bajas",
                    "Sucursal " + sucursal,
                    bajas
            );
        }
    }

    private void detectarMargenNormalizado() {
        List<MovimientoFinancieroFuenteDTO> movimientos = finanzasClient.obtenerMovimientos();
        LocalDate hoy = LocalDate.now();
        LocalDate inicioActual = hoy.withDayOfMonth(1);
        LocalDate finAnterior = inicioActual.minusDays(1);
        LocalDate inicioAnterior = finAnterior.withDayOfMonth(1);

        BigDecimal margenActual = sumarMargen(movimientos, inicioActual, hoy);
        BigDecimal margenAnterior = sumarMargen(movimientos, inicioAnterior, finAnterior);

        boolean normalizado = margenAnterior.compareTo(BigDecimal.ZERO) < 0
                && margenActual.compareTo(BigDecimal.ZERO) >= 0;

        registrarSiCorresponde(
                "MARGEN_NORMALIZADO:FINANZAS",
                Alerta.TipoAlerta.MARGEN_NORMALIZADO,
                "Margen normalizado",
                "Finanzas",
                normalizado
        );
    }

    private Map<String, BigDecimal> sumarPorSucursal(List<VentaFuenteDTO> ventas, LocalDate inicio, LocalDate fin) {
        Map<String, BigDecimal> totales = new HashMap<>();
        for (VentaFuenteDTO venta : ventas) {
            if (venta.getFecha() == null || venta.getSucursal() == null) {
                continue;
            }
            if (venta.getFecha().isBefore(inicio) || venta.getFecha().isAfter(fin)) {
                continue;
            }
            BigDecimal monto = venta.getMontoTotal() == null ? BigDecimal.ZERO : BigDecimal.valueOf(venta.getMontoTotal());
            String sucursal = venta.getSucursal().trim();
            totales.put(sucursal, totales.getOrDefault(sucursal, BigDecimal.ZERO).add(monto));
        }
        return totales;
    }

    private BigDecimal sumarMargen(List<MovimientoFinancieroFuenteDTO> movimientos, LocalDate inicio, LocalDate fin) {
        BigDecimal total = BigDecimal.ZERO;
        for (MovimientoFinancieroFuenteDTO movimiento : movimientos) {
            if (movimiento.getFechaRegistro() == null) {
                continue;
            }
            if (movimiento.getFechaRegistro().isBefore(inicio) || movimiento.getFechaRegistro().isAfter(fin)) {
                continue;
            }
            if (movimiento.getMargen() != null) {
                total = total.add(movimiento.getMargen());
            }
        }
        return total;
    }

    private void registrarSiCorresponde(String codigo, Alerta.TipoAlerta tipo, String titulo, String detalle, boolean disparar) {
        if (!disparar) {
            return;
        }

        alertaRepository.findByCodigo(codigo).ifPresentOrElse(alerta -> {
            alerta.setTipo(tipo);
            alerta.setTitulo(titulo);
            alerta.setDetalle(detalle);
            alertaRepository.save(alerta);
        }, () -> {
            Alerta alerta = new Alerta();
            alerta.setCodigo(codigo);
            alerta.setTipo(tipo);
            alerta.setTitulo(titulo);
            alerta.setDetalle(detalle);
            alerta.setLeida(false);
            alerta.setFechaCreacion(java.time.LocalDateTime.now());
            alertaRepository.save(alerta);
        });
    }
}
