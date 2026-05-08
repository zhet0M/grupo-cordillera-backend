package com.grupocordillera.finanzas.dto;

import lombok.Data;

@Data
public class ProductoInventarioDTO {
    private Long id;
    private String sku;
    private String nombre;
    private Double costo;
    private Integer stock;
    private String sucursal;
}
