package com.grupocordillera.clientes.controller;

import com.grupocordillera.clientes.dto.ClienteRequest;
import com.grupocordillera.clientes.dto.RegistrarCompraRequest;
import com.grupocordillera.clientes.model.Cliente;
import com.grupocordillera.clientes.service.ClientesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Gestión de clientes y compras")
@RequiredArgsConstructor
public class ClientesController {

    private final ClientesService clientesService;

    @PostMapping
    public ResponseEntity<Cliente> crear(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clientesService.crearCliente(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clientesService.actualizarCliente(id, request));
    }

    @PutMapping("/{id}/compras")
    public ResponseEntity<Cliente> registrarCompra(@PathVariable Long id, @Valid @RequestBody RegistrarCompraRequest request) {
        return ResponseEntity.ok(clientesService.registrarCompra(id, request));
    }

    @GetMapping
    public ResponseEntity<List<Cliente>> obtenerTodos() {
        return ResponseEntity.ok(clientesService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clientesService.obtenerPorId(id));
    }

    @GetMapping("/tipo/{tipoCliente}")
    public ResponseEntity<List<Cliente>> obtenerPorTipo(@PathVariable Cliente.TipoCliente tipoCliente) {
        return ResponseEntity.ok(clientesService.obtenerPorTipo(tipoCliente));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Cliente>> obtenerPorEstado(@PathVariable Cliente.Estado estado) {
        return ResponseEntity.ok(clientesService.obtenerPorEstado(estado));
    }
}
