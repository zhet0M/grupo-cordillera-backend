package com.grupocordillera.ventas.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MovimientoFinancieroRequest {

    private Long ventaId;
    private Long productoId;
    private String skuProducto;
    private String nombreProducto;
    private Integer cantidad;
    private BigDecimal ingresos;
    private LocalDate fechaRegistro;
    private String sucursal;
    private TipoMovimiento tipoMovimiento;

    public enum TipoMovimiento {
        INGRESO,
        EGRESO,
        AJUSTE
    }
}
