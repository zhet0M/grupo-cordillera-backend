package com.grupocordillera.inventario.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@DiscriminatorValue("HOGAR")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductoHogar extends Producto {
    
    // Atributos específicos de hogar
    private String material;
    private String dimensiones;
}
