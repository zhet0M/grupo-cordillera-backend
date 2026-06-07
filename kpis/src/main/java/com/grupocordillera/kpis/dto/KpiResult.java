package com.grupocordillera.kpis.dto;

import com.grupocordillera.kpis.enums.KpiEstado;
import com.grupocordillera.kpis.enums.KpiType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class KpiResult {
    private KpiType tipo;
    private BigDecimal valor = BigDecimal.ZERO;
    private BigDecimal variacion = BigDecimal.ZERO;
    private KpiEstado estado = KpiEstado.NEUTRO;
    private String periodo;
    private String descripcion;
    private String fuenteDatos;
    private Map<String, BigDecimal> detalles = new LinkedHashMap<>();
}
