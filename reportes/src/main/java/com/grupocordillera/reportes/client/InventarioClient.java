package com.grupocordillera.reportes.client;

import com.grupocordillera.reportes.dto.ProductoReporteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "inventario-reportes", url = "${app.services.inventario-url:http://localhost:8083/inventario}")
public interface InventarioClient {

    @GetMapping
    List<ProductoReporteDTO> obtenerTodos();
}
