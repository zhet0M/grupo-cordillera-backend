package com.grupocordillera.alertas.client;

import com.grupocordillera.alertas.dto.ProductoInventarioFuenteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "inventario-alertas", url = "${app.services.inventario-url:http://localhost:8083/inventario}")
public interface InventarioClient {
    @GetMapping
    List<ProductoInventarioFuenteDTO> obtenerProductos();
}
