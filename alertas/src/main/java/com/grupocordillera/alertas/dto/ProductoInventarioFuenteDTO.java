package com.grupocordillera.alertas.dto;

import lombok.Data;

@Data
public class ProductoInventarioFuenteDTO {
    private Long id;
    private String sku;
    private String nombre;
    private Integer stock;
    private Integer stockMinimo;
    private String estado;
    private String sucursal;
}
