package com.grupocordillera.kpis.controller;

import com.grupocordillera.kpis.dto.KpiResult;
import com.grupocordillera.kpis.enums.KpiType;
import com.grupocordillera.kpis.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/kpis")
@RequiredArgsConstructor
public class KpisController {

    private final KpiService kpiService;

    @GetMapping
    public ResponseEntity<List<KpiResult>> obtenerResumen() {
        return ResponseEntity.ok(kpiService.obtenerResumen());
    }

    @GetMapping("/{tipo}")
    public ResponseEntity<KpiResult> obtenerPorTipo(@PathVariable String tipo) {
        return ResponseEntity.ok(kpiService.obtenerPorTipo(KpiType.from(tipo)));
    }
}
