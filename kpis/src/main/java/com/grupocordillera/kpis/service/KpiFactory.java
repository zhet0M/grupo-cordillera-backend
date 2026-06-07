package com.grupocordillera.kpis.service;

import com.grupocordillera.kpis.enums.KpiType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KpiFactory {

    private final List<KpiCalculator> calculators;

    public KpiCalculator crear(KpiType tipo) {
        return calculators.stream()
                .filter(calculator -> calculator.supports(tipo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe calculadora para el KPI: " + tipo));
    }
}
