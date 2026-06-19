package com.grupocordillera.finanzas.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.grupocordillera.finanzas.dto.MovimientoFinancieroRequest;
import com.grupocordillera.finanzas.dto.ResumenFinancieroDTO;
import com.grupocordillera.finanzas.model.MovimientoFinanciero;
import com.grupocordillera.finanzas.service.FinanzasService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/finanzas")
@Tag(name = "Finanzas", description = "Movimientos, totales y resúmenes financieros")
@RequiredArgsConstructor
public class FinanzasController {

    private final FinanzasService finanzasService;

    @PostMapping
    public ResponseEntity<MovimientoFinanciero> registrar(@Valid @RequestBody MovimientoFinancieroRequest request) {
        return ResponseEntity.ok(finanzasService.registrarMovimiento(request));
    }

    @GetMapping
    public ResponseEntity<List<MovimientoFinanciero>> obtenerTodos() {
        return ResponseEntity.ok(finanzasService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovimientoFinanciero> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(finanzasService.obtenerPorId(id));
    }

    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<MovimientoFinanciero>> obtenerPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(finanzasService.obtenerPorFecha(fecha));
    }

    @GetMapping("/sucursal/{sucursal}")
    public ResponseEntity<List<MovimientoFinanciero>> obtenerPorSucursal(@PathVariable String sucursal) {
        return ResponseEntity.ok(finanzasService.obtenerPorSucursal(sucursal));
    }

    @GetMapping("/tipo/{tipoMovimiento}")
    public ResponseEntity<List<MovimientoFinanciero>> obtenerPorTipo(
            @PathVariable MovimientoFinanciero.TipoMovimiento tipoMovimiento) {
        return ResponseEntity.ok(finanzasService.obtenerPorTipo(tipoMovimiento));
    }

    @GetMapping("/rango")
    public ResponseEntity<List<MovimientoFinanciero>> obtenerPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(finanzasService.obtenerPorRango(inicio, fin));
    }

    @GetMapping("/total")
    public ResponseEntity<ResumenFinancieroDTO> obtenerTotalesGenerales() {
        return ResponseEntity.ok(finanzasService.obtenerTotalesGenerales());
    }

    @GetMapping("/total/rango")
    public ResponseEntity<ResumenFinancieroDTO> obtenerTotalesPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(finanzasService.obtenerTotalesPorRango(inicio, fin));
    }

    @GetMapping("/total/sucursal/{sucursal}")
    public ResponseEntity<ResumenFinancieroDTO> obtenerTotalesPorSucursal(@PathVariable String sucursal) {
        return ResponseEntity.ok(finanzasService.obtenerTotalesPorSucursal(sucursal));
    }
}
