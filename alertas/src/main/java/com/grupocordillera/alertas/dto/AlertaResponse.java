package com.grupocordillera.alertas.dto;

import com.grupocordillera.alertas.model.Alerta;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertaResponse {
    private Long id;
    private Alerta.TipoAlerta tipo;
    private String icono;
    private String titulo;
    private String detalle;
    private String tituloCompleto;
    private Boolean leida;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLectura;
    private String tiempoRelativo;
}
