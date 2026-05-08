package com.grupocordillera.finanzas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.grupocordillera.finanzas.model.MovimientoFinanciero;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MovimientoFinancieroRequest {

    private Long ventaId;

    private Long productoId;

    @NotBlank
    private String skuProducto;

    private String nombreProducto;

    @NotNull
    @Positive
    private Integer cantidad;

    @NotNull
    @Positive
    private BigDecimal ingresos;

    private BigDecimal costo;

    private LocalDate fechaRegistro;

    @NotBlank
    private String sucursal;

    @NotNull
    private MovimientoFinanciero.TipoMovimiento tipoMovimiento;
}
