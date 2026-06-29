package com.grupocordillera.alertas.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupocordillera.alertas.client.FinanzasClient;
import com.grupocordillera.alertas.client.InventarioClient;
import com.grupocordillera.alertas.client.VentasClient;
import com.grupocordillera.alertas.dto.MovimientoFinancieroFuenteDTO;
import com.grupocordillera.alertas.dto.ProductoInventarioFuenteDTO;
import com.grupocordillera.alertas.dto.VentaFuenteDTO;
import com.grupocordillera.alertas.model.Alerta;
import com.grupocordillera.alertas.repository.AlertaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeteccionAlertasService {

    private final AlertaRepository alertaRepository;
    private final InventarioClient inventarioClient;
    private final VentasClient ventasClient;
    private final FinanzasClient finanzasClient;

    @Scheduled(fixedDelayString = "${alertas.deteccion.delay-ms:60000}")
    public void detectarAutomaticamente() {
        detectarStockCritico();
        detectarVentasBajas();
        detectarMargenNormalizado();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void detectarAlArranque() {
        detectarAutomaticamente();
    }

    public void detectarAhora() {
        detectarAutomaticamente();
    }

    private void detectarStockCritico() {
        try {
            System.out.println("Iniciando detección de stock crítico...");
            
            // Obtener respuesta raw y parsearla manualmente para evitar problemas de polimorfismo
            ResponseEntity<String> rawResponse = inventarioClient.obtenerProductosRaw();
            String json = rawResponse.getBody();
            
            System.out.println("Respuesta RAW obtenida: " + (json != null ? json.length() + " caracteres" : "null"));
            
            // Configurar ObjectMapper para ignorar información de tipo
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
            
            List<ProductoInventarioFuenteDTO> productos = objectMapper.readValue(json, 
                new TypeReference<List<ProductoInventarioFuenteDTO>>() {});
            
            System.out.println("Obtenidos " + productos.size() + " productos del servicio de inventario");
            
            for (ProductoInventarioFuenteDTO producto : productos) {
                System.out.println("Revisando producto: SKU=" + producto.getSku() + 
                    ", Nombre=" + producto.getNombre() + 
                    ", Stock=" + producto.getStock() + 
                    ", StockMinimo=" + producto.getStockMinimo() + 
                    ", Estado=" + producto.getEstado());
                
                if (producto.getSku() == null || producto.getNombre() == null) {
                    System.out.println("  Saltando producto (sin SKU o nombre)");
                    continue;
                }
                
                boolean critico = producto.getStock() != null && producto.getStockMinimo() != null
                        && producto.getStock() <= producto.getStockMinimo()
                        && (producto.getEstado() == null || !"DESCONTINUADO".equalsIgnoreCase(producto.getEstado()));
                
                System.out.println("  Stock crítico? " + critico);
                
                registrarSiCorresponde(
                        "STOCK_CRITICO:" + producto.getSku(),
                        Alerta.TipoAlerta.STOCK_CRITICO,
                        "Stock crítico",
                        producto.getNombre(),
                        critico
                );
            }
        } catch (Exception e) {
            System.err.println("Error al detectar stock crítico: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void detectarVentasBajas() {
        try {
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
        } catch (Exception e) {
            System.err.println("Error al detectar ventas bajas: " + e.getMessage());
        }
    }

    private void detectarMargenNormalizado() {
        try {
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
        } catch (Exception e) {
            System.err.println("Error al detectar margen normalizado: " + e.getMessage());
        }
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
        System.out.println("registrarSiCorresponde - Codigo: " + codigo + ", Disparar: " + disparar);
        if (!disparar) {
            System.out.println("  No se dispara alerta");
            return;
        }

        alertaRepository.findByCodigo(codigo).ifPresentOrElse(alerta -> {
            System.out.println("  Actualizando alerta existente: " + codigo);
            alerta.setTipo(tipo);
            alerta.setTitulo(titulo);
            alerta.setDetalle(detalle);
            // Volver a marcar como no leída si el problema sigue presente
            alerta.setLeida(false);
            alerta.setFechaLectura(null);
            alertaRepository.save(alerta);
            System.out.println("  Alerta actualizada y marcada como no leída!");
        }, () -> {
            System.out.println("  Creando nueva alerta: " + codigo);
            Alerta alerta = new Alerta();
            alerta.setCodigo(codigo);
            alerta.setTipo(tipo);
            alerta.setTitulo(titulo);
            alerta.setDetalle(detalle);
            alerta.setLeida(false);
            alerta.setFechaCreacion(java.time.LocalDateTime.now());
            alertaRepository.save(alerta);
            System.out.println("  Alerta guardada exitosamente!");
        });
    }
}
