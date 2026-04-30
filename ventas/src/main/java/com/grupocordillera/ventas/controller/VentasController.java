package com.grupocordillera.ventas.controller;

import com.grupocordillera.ventas.model.Venta;
import com.grupocordillera.ventas.service.VentasService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class VentasController {

    private final VentasService ventasService;

    @PostMapping
    public ResponseEntity<Venta> registrar(@RequestBody Venta venta) {
        return ResponseEntity.ok(ventasService.registrarVenta(venta));
    }

    @GetMapping
    public ResponseEntity<List<Venta>> obtenerTodas() {
        return ResponseEntity.ok(ventasService.obtenerTodas());
    }

    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<Venta>> porFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha) {
        return ResponseEntity.ok(ventasService.obtenerPorFecha(fecha));
    }

    // get ventas de hoy
    @GetMapping("/hoy")
    public ResponseEntity<List<Venta>> hoy() {
        return ResponseEntity.ok(ventasService.obtenerHoy());
    }

    // get por sucursal
    @GetMapping("/sucursal/{sucursal}")
    public ResponseEntity<List<Venta>> porSucursal(
            @PathVariable String sucursal) {
        return ResponseEntity.ok(ventasService.obtenerPorSucursal(sucursal));
    }

    // get ventas por rango de fechas
    @GetMapping("/rango")
    public ResponseEntity<List<Venta>> porRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fin) {
        return ResponseEntity.ok(ventasService.obtenerPorRango(inicio, fin));
    }

    // get total ventas por período
    @GetMapping("/total/periodo")
    public ResponseEntity<Double> totalPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fin) {
        return ResponseEntity.ok(ventasService.totalPorPeriodo(inicio, fin));
    }

    // get total de ventas por sucursal
    @GetMapping("/total/sucursal/{sucursal}")
    public ResponseEntity<Double> totalSucursal(
            @PathVariable String sucursal) {
        return ResponseEntity.ok(ventasService.totalPorSucursal(sucursal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ventasService.eliminarVenta(id);
        return ResponseEntity.noContent().build();
    }
}
