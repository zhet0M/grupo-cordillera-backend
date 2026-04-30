package com.autenticacion.authentication.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    public enum Rol {
        SUPER_ADMIN, ADMIN_VENTAS, ADMIN_INVENTARIO, ADMIN_FINANZAS, ADMIN_CLIENTES, EJECUTIVO, ANALISTA
    }

    public enum Estado {
        PENDIENTE, APROBADO, RECHAZADO, BLOQUEADO
    }
}
