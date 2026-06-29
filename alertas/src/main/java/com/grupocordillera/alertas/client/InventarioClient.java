package com.grupocordillera.alertas.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import com.grupocordillera.alertas.dto.ProductoInventarioFuenteDTO;

@FeignClient(name = "inventario-alertas", url = "${app.services.inventario-url:http://localhost:8083/inventario}")
public interface InventarioClient {
    @GetMapping
    ResponseEntity<String> obtenerProductosRaw();
    
    @GetMapping
    List<ProductoInventarioFuenteDTO> obtenerProductos();
}
