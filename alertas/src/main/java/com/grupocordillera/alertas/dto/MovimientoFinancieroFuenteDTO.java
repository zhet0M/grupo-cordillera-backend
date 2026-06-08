package com.grupocordillera.alertas.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MovimientoFinancieroFuenteDTO {
    private Long id;
    private BigDecimal ingresos;
    private BigDecimal costo;
    private BigDecimal margen;
    private LocalDate fechaRegistro;
    private String sucursal;
}
