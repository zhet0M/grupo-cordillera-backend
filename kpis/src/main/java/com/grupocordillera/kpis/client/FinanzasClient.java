package com.grupocordillera.kpis.client;

import com.grupocordillera.kpis.model.MovimientoFinancieroKpiDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "finanzas", url = "${app.services.finanzas-url}")
public interface FinanzasClient {

    @GetMapping
    List<MovimientoFinancieroKpiDTO> obtenerMovimientos();
}
