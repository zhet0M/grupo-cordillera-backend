package com.grupocordillera.clientes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RegistrarCompraRequest {

    @NotNull
    @Positive
    private BigDecimal montoCompra;

    private LocalDate fechaCompra;
}
