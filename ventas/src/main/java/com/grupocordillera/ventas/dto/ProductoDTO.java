package com.grupocordillera.ventas.dto;

import lombok.Data;

@Data
public class ProductoDTO {
    private Long id;
    private String sku;
    private String nombre;
    private Double precio;
    private Integer stock;
}
