package com.grupocordillera.alertas.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VentaFuenteDTO {
    private Long id;
    private String sucursal;
    private Double montoTotal;
    private Integer cantidad;
    private LocalDate fecha;
}
