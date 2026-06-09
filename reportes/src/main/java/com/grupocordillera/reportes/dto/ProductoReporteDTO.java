package com.grupocordillera.reportes.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProductoReporteDTO {
    private Long id;
    private String sku;
    private String nombre;
    private String descripcion;
    private String categoria;
    private String marca;
    private String modelo;
    private Double precio;
    private Double costo;
    private Integer stock;
    private Integer stockMinimo;
    private String sucursal;
    private String estado;
    private LocalDate fechaIngreso;
}
