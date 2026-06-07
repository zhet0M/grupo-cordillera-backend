package com.grupocordillera.kpis.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VentaKpiDTO {
    private Long id;
    private String sucursal;
    private Long clienteId;
    private Long productoId;
    private String skuProducto;
    private String nombreProducto;
    private Double precioUnitario;
    private Integer cantidad;
    private Double montoTotal;
    private LocalDate fecha;
    private String canal;
    private String estadoFinanzas;
}
