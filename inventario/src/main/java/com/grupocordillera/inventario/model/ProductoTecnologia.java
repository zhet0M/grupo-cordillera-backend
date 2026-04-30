package com.grupocordillera.inventario.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@DiscriminatorValue("TECNOLOGIA")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductoTecnologia extends Producto {
    
    // Atributos específicos de tecnología
    private Integer mesesGarantia;
    private String voltaje;
}
