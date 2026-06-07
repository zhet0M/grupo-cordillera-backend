package com.grupocordillera.kpis.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ClienteKpiDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String tipoCliente;
    private String estado;
    private LocalDate fechaRegistro;
    private Integer cantidadCompras;
    private BigDecimal montoAcumulado;
    private LocalDate ultimaFechaCompra;
}
