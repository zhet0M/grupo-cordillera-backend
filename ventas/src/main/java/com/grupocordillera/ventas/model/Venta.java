package com.grupocordillera.ventas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
@Data
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sucursal;

    private Long clienteId;

    private String nombreCliente;

    private String apellidoCliente;

    private String emailCliente;

    private String telefonoCliente;

    private String direccionCliente;

    @Enumerated(EnumType.STRING)
    private TipoClienteSnapshot tipoClienteSnapshot;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoFinanzas estadoFinanzas;

    @Column(nullable = false)
    private Integer intentosFinanzas;

    @Column(length = 1000)
    private String ultimoErrorFinanzas;

    private LocalDateTime fechaUltimoIntentoFinanzas;

    public enum Canal {
        POS,        // punto de venta físico
        ECOMMERCE   // venta online
    }

    public enum EstadoFinanzas {
        PENDIENTE,
        SINCRONIZADO,
        ERROR
    }

    public enum TipoClienteSnapshot {
        REGULAR,
        FRECUENTE,
        VIP,
        CORPORATIVO,
        MAYORISTA
    }
}
