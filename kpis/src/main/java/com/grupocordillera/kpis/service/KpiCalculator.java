package com.grupocordillera.kpis.service;

import com.grupocordillera.kpis.dto.KpiResult;
import com.grupocordillera.kpis.enums.KpiType;

public interface KpiCalculator {

    boolean supports(KpiType tipo);

    KpiResult calcular(KpiType tipo);
}
