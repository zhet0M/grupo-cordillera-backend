package com.grupocordillera.kpis.service;

import com.grupocordillera.kpis.dto.KpiResult;
import com.grupocordillera.kpis.enums.KpiEstado;
import com.grupocordillera.kpis.enums.KpiType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KpiService {

    private final KpiFactory kpiFactory;

    public List<KpiResult> obtenerResumen() {
        return Arrays.stream(KpiType.values())
                .map(tipo -> calcularSeguro(tipo))
                .toList();
    }

    public KpiResult obtenerPorTipo(KpiType tipo) {
        return calcularSeguro(tipo);
    }

    private KpiResult calcularSeguro(KpiType tipo) {
        try {
            return kpiFactory.crear(tipo).calcular(tipo);
        } catch (Exception ex) {
            KpiResult error = new KpiResult();
            error.setTipo(tipo);
            error.setValor(BigDecimal.ZERO);
            error.setVariacion(BigDecimal.ZERO);
            error.setEstado(KpiEstado.SIN_DATOS);
            error.setPeriodo(LocalDateTime.now().toString());
            error.setDescripcion("No fue posible calcular este KPI en este momento");
            error.setFuenteDatos("ERROR");
            return error;
        }
    }
}
