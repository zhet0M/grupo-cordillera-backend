package com.grupocordillera.alertas.client;

import com.grupocordillera.alertas.dto.MovimientoFinancieroFuenteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "finanzas-alertas", url = "${app.services.finanzas-url:http://localhost:8084/finanzas}")
public interface FinanzasClient {
    @GetMapping
    List<MovimientoFinancieroFuenteDTO> obtenerMovimientos();
}
