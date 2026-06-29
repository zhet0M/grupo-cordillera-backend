package com.grupocordillera.alertas.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grupocordillera.alertas.client.InventarioClient;
import com.grupocordillera.alertas.dto.AlertaResponse;
import com.grupocordillera.alertas.dto.AlertasResumenResponse;
import com.grupocordillera.alertas.dto.CrearAlertaRequest;
import com.grupocordillera.alertas.dto.ProductoInventarioFuenteDTO;
import com.grupocordillera.alertas.service.AlertaService;
import com.grupocordillera.alertas.service.DeteccionAlertasService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/alertas")
@Tag(name = "Alertas", description = "Creación, consulta y detección de alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;
    private final DeteccionAlertasService deteccionAlertasService;
    private final InventarioClient inventarioClient;

    @PostMapping
    public ResponseEntity<AlertaResponse> crear(@Valid @RequestBody CrearAlertaRequest request) {
        return ResponseEntity.ok(alertaService.crearAlerta(request));
    }

    @GetMapping
    public ResponseEntity<List<AlertaResponse>> obtenerTodas() {
        return ResponseEntity.ok(alertaService.obtenerTodas());
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<List<AlertaResponse>> obtenerNoLeidas() {
        return ResponseEntity.ok(alertaService.obtenerNoLeidas());
    }

    @GetMapping("/no-leidas/count")
    public ResponseEntity<Long> contarNoLeidas() {
        return ResponseEntity.ok(alertaService.contarNoLeidas());
    }

    @GetMapping("/resumen")
    public ResponseEntity<AlertasResumenResponse> obtenerResumen() {
        return ResponseEntity.ok(alertaService.obtenerResumen());
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<AlertaResponse> marcarComoLeida(@PathVariable Long id) {
        return ResponseEntity.ok(alertaService.marcarComoLeida(id));
    }

    @PutMapping("/leer-todas")
    public ResponseEntity<List<AlertaResponse>> marcarTodasComoLeidas() {
        return ResponseEntity.ok(alertaService.marcarTodasComoLeidas());
    }

    @PostMapping("/detectar")
    public ResponseEntity<String> detectarAhora() {
        deteccionAlertasService.detectarAhora();
        return ResponseEntity.ok("Deteccion ejecutada");
    }

    @GetMapping("/test-inventario")
    public ResponseEntity<String> testInventario() {
        ResponseEntity<String> rawResponse = inventarioClient.obtenerProductosRaw();
        System.out.println("Test Inventario - Respuesta RAW:");
        System.out.println(rawResponse.getBody());
        
        try {
            List<ProductoInventarioFuenteDTO> productos = inventarioClient.obtenerProductos();
            System.out.println("\nTest Inventario - Productos deserializados:");
            for (ProductoInventarioFuenteDTO p : productos) {
                System.out.println("  SKU: " + p.getSku() + 
                    ", Stock: " + p.getStock() + 
                    ", StockMin: " + p.getStockMinimo() + 
                    ", Estado: " + p.getEstado());
            }
            System.out.println("Total de productos deserializados: " + productos.size());
        } catch (Exception e) {
            System.err.println("Error al deserializar productos:");
            e.printStackTrace();
        }
        
        return rawResponse;
    }
}
