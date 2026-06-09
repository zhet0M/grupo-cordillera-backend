package com.grupocordillera.reportes.client;

import com.grupocordillera.reportes.dto.VentaReporteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "ventas-reportes", url = "${app.services.ventas-url:http://localhost:8082/ventas}")
public interface VentasClient {

    @GetMapping("/rango")
    List<VentaReporteDTO> obtenerPorRango(
            @RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam("fin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin);
}
