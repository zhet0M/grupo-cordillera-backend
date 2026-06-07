package com.grupocordillera.kpis.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MovimientoFinancieroKpiDTO {
    private Long id;
    private Long ventaId;
    private Long productoId;
    private String skuProducto;
    private String nombreProducto;
    private Integer cantidad;
    private BigDecimal ingresos;
    private BigDecimal costo;
    private BigDecimal margen;
    private LocalDate fechaRegistro;
    private String sucursal;
    private String tipoMovimiento;
}
