package com.grupocordillera.alertas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductoInventarioFuenteDTO {
    private Long id;
    private String sku;
    private String nombre;
    private Integer stock;
    private Integer stockMinimo;
    private String estado;
    private String sucursal;
    private String categoria;
}
