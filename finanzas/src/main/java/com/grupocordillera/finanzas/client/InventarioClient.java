package com.grupocordillera.finanzas.client;

import com.grupocordillera.finanzas.dto.ProductoInventarioDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inventario-finanzas", url = "http://inventario:8083/inventario")
public interface InventarioClient {

    @GetMapping("/sku/{sku}")
    ProductoInventarioDTO obtenerPorSku(@PathVariable("sku") String sku);
}
