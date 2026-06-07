package com.grupocordillera.kpis.client;

import com.grupocordillera.kpis.model.VentaKpiDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "ventas", url = "${app.services.ventas-url}")
public interface VentasClient {

    @GetMapping
    List<VentaKpiDTO> obtenerVentas();
}
