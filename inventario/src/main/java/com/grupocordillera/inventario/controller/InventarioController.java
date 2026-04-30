package com.grupocordillera.inventario.controller;

import com.grupocordillera.inventario.dto.DescuentoStockRequest;
import com.grupocordillera.inventario.model.Producto;
import com.grupocordillera.inventario.service.InventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    @PostMapping
    public ResponseEntity<Producto> registrar(@RequestBody Producto producto) {
        return ResponseEntity.ok(inventarioService.registrarProducto(producto));
    }

    @GetMapping
    public ResponseEntity<List<Producto>> obtenerTodos() {
        return ResponseEntity.ok(inventarioService.obtenerTodos());
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<Producto> porSku(@PathVariable String sku) {
        return ResponseEntity.ok(inventarioService.obtenerPorSku(sku));
    }

    @GetMapping("/sucursal/{sucursal}")
    public ResponseEntity<List<Producto>> porSucursal(@PathVariable String sucursal) {
        return ResponseEntity.ok(inventarioService.obtenerPorSucursal(sucursal));
    }

    // Endpoint para que Ventas descuente stock usando Circuit Breaker
    @PutMapping("/descontar/{sku}")
    public ResponseEntity<Producto> descontarStock(@PathVariable String sku, @RequestBody DescuentoStockRequest request) {
        return ResponseEntity.ok(inventarioService.descontarStock(sku, request.getCantidad()));
    }

    // Variante sin body para llamadas internas entre microservicios
    @PutMapping("/descontar/{sku}/{cantidad}")
    public ResponseEntity<Producto> descontarStockDirecto(@PathVariable String sku, @PathVariable int cantidad) {
        return ResponseEntity.ok(inventarioService.descontarStock(sku, cantidad));
    }
}
