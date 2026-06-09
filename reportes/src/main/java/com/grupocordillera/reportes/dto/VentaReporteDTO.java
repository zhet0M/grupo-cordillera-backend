package com.grupocordillera.reportes.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VentaReporteDTO {
    private Long id;
    private String sucursal;
    private Long clienteId;
    private String nombreCliente;
    private String apellidoCliente;
    private String emailCliente;
    private String telefonoCliente;
    private String direccionCliente;
    private String tipoClienteSnapshot;
    private Long productoId;
    private String skuProducto;
    private String nombreProducto;
    private Double precioUnitario;
    private Integer cantidad;
    private Double montoTotal;
    private LocalDate fecha;
    private String canal;
    private String estadoFinanzas;
    private Integer intentosFinanzas;
    private String ultimoErrorFinanzas;
}
