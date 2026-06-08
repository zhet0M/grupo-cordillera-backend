package com.grupocordillera.alertas.dto;

import com.grupocordillera.alertas.model.Alerta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearAlertaRequest {

    private String codigo;

    @NotNull
    private Alerta.TipoAlerta tipo;

    @NotBlank
    private String titulo;

    @NotBlank
    private String detalle;
}
