package com.grupocordillera.ventas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "ventas")
@Data
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sucursal;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private String skuProducto;

    @Column(nullable = false)
    private String nombreProducto;

    @Column(nullable = false)
    private Double precioUnitario;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Double montoTotal;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    private Canal canal;

    public enum Canal {
        POS,        // punto de venta físico
        ECOMMERCE   // venta online
    }
}
