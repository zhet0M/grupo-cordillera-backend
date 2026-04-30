package com.grupocordillera.inventario.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Entity
@Table(name = "productos")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_producto", discriminatorType = DiscriminatorType.STRING)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "categoria",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ProductoTecnologia.class, name = "TECNOLOGIA"),
        @JsonSubTypes.Type(value = ProductoHogar.class, name = "HOGAR")
})
@Data
public abstract class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sku; // Identificador único comercial

    @Column(nullable = false)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private String categoria;

    private String marca;
    private String modelo;

    @Column(nullable = false)
    private Double precio;

    @Column(nullable = false)
    private Double costo;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Integer stockMinimo;

    @Column(nullable = false)
    private String sucursal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    @Column(nullable = false)
    private LocalDate fechaIngreso;

    public enum Estado {
        DISPONIBLE, AGOTADO, DESCONTINUADO
    }
}
