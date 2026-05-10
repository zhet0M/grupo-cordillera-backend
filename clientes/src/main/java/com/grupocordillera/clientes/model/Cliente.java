package com.grupocordillera.clientes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "clientes")
@Data
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCliente tipoCliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    @Column(nullable = false)
    private LocalDate fechaRegistro;

    @Column(nullable = false)
    private Integer cantidadCompras;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montoAcumulado;

    private LocalDate ultimaFechaCompra;

    public enum TipoCliente {
        REGULAR,
        FRECUENTE,
        VIP,
        CORPORATIVO,
        MAYORISTA
    }

    public enum Estado {
        ACTIVO,
        INACTIVO
    }
}
