package com.grupocordillera.alertas.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AlertasResumenResponse {
    private long noLeidas;
    private List<AlertaResponse> alertas;
}
