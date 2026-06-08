package com.grupocordillera.alertas.client;

import com.grupocordillera.alertas.dto.VentaFuenteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "ventas-alertas", url = "${app.services.ventas-url:http://localhost:8082/ventas}")
public interface VentasClient {
    @GetMapping
    List<VentaFuenteDTO> obtenerVentas();
}
