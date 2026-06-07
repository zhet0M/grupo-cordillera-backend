package com.grupocordillera.kpis.client;

import com.grupocordillera.kpis.model.ProductoKpiDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "inventario", url = "${app.services.inventario-url}")
public interface InventarioClient {

    @GetMapping
    List<ProductoKpiDTO> obtenerProductos();
}
