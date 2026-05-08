package com.grupocordillera.finanzas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ResumenFinancieroDTO {
    private BigDecimal ingresos;
    private BigDecimal costo;
    private BigDecimal margen;
}
