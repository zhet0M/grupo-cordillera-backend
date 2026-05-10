package com.grupocordillera.ventas.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RegistrarCompraClienteRequest {

    private BigDecimal montoCompra;
    private LocalDate fechaCompra;
}
