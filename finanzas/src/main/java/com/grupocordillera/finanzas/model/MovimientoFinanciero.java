package com.grupocordillera.finanzas.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "movimientos_financieros")
@Data
public class MovimientoFinanciero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ventaId;

    private Long productoId;

    private String skuProducto;

    private String nombreProducto;

    private Integer cantidad;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal ingresos;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal costo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal margen;

    @Column(nullable = false)
    private LocalDate fechaRegistro;

    @Column(nullable = false)
    private String sucursal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipoMovimiento;

    public enum TipoMovimiento {
        INGRESO,
        EGRESO,
        AJUSTE
    }
}
