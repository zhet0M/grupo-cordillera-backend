package com.grupocordillera.reportes.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResumenFinancieroDTO {
    private BigDecimal ingresos;
    private BigDecimal costo;
    private BigDecimal margen;
}
