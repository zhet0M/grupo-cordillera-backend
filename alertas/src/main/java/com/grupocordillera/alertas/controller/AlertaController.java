package com.grupocordillera.alertas.controller;

import com.grupocordillera.alertas.dto.AlertaResponse;
import com.grupocordillera.alertas.dto.AlertasResumenResponse;
import com.grupocordillera.alertas.dto.CrearAlertaRequest;
import com.grupocordillera.alertas.service.DeteccionAlertasService;
import com.grupocordillera.alertas.service.AlertaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;
    private final DeteccionAlertasService deteccionAlertasService;

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
}
