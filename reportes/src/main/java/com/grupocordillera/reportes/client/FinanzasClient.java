package com.grupocordillera.reportes.client;

import com.grupocordillera.reportes.dto.MovimientoFinancieroReporteDTO;
import com.grupocordillera.reportes.dto.ResumenFinancieroDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "finanzas-reportes", url = "${app.services.finanzas-url:http://localhost:8084/finanzas}")
public interface FinanzasClient {

    @GetMapping("/rango")
    List<MovimientoFinancieroReporteDTO> obtenerPorRango(
            @RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam("fin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin);

    @GetMapping("/total/rango")
    ResumenFinancieroDTO obtenerTotalesPorRango(
            @RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam("fin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin);
}
