package com.grupocordillera.alertas.service;

import com.grupocordillera.alertas.dto.AlertaResponse;
import com.grupocordillera.alertas.dto.AlertasResumenResponse;
import com.grupocordillera.alertas.dto.CrearAlertaRequest;
import com.grupocordillera.alertas.model.Alerta;
import com.grupocordillera.alertas.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;

    public AlertaResponse crearAlerta(CrearAlertaRequest request) {
        String codigo = request.getCodigo() != null && !request.getCodigo().isBlank()
                ? request.getCodigo().trim()
                : "MANUAL:" + request.getTipo() + ":" + request.getTitulo().trim();

        Alerta alerta = alertaRepository.findByCodigo(codigo).orElseGet(Alerta::new);
        alerta.setCodigo(codigo);
        alerta.setTipo(request.getTipo());
        alerta.setTitulo(request.getTitulo().trim());
        alerta.setDetalle(request.getDetalle().trim());
        alerta.setLeida(false);
        alerta.setFechaLectura(null);
        if (alerta.getFechaCreacion() == null) {
            alerta.setFechaCreacion(LocalDateTime.now());
        }

        return toResponse(alertaRepository.save(alerta));
    }

    public List<AlertaResponse> obtenerTodas() {
        return alertaRepository.findAllByOrderByFechaCreacionDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AlertaResponse> obtenerNoLeidas() {
        return alertaRepository.findByLeidaFalseOrderByFechaCreacionDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public long contarNoLeidas() {
        return alertaRepository.countByLeidaFalse();
    }

    public AlertasResumenResponse obtenerResumen() {
        return AlertasResumenResponse.builder()
                .noLeidas(contarNoLeidas())
                .alertas(obtenerNoLeidas())
                .build();
    }

    public AlertaResponse marcarComoLeida(Long id) {
        Alerta alerta = obtenerEntidad(id);
        if (Boolean.FALSE.equals(alerta.getLeida())) {
            alerta.setLeida(true);
            alerta.setFechaLectura(LocalDateTime.now());
            alerta = alertaRepository.save(alerta);
        }
        return toResponse(alerta);
    }

    public List<AlertaResponse> marcarTodasComoLeidas() {
        List<Alerta> alertas = alertaRepository.findByLeidaFalseOrderByFechaCreacionDesc();
        for (Alerta alerta : alertas) {
            alerta.setLeida(true);
            alerta.setFechaLectura(LocalDateTime.now());
        }
        return alertaRepository.saveAll(alertas).stream()
                .map(this::toResponse)
                .toList();
    }

    private Alerta obtenerEntidad(Long id) {
        return alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada con id: " + id));
    }

    private AlertaResponse toResponse(Alerta alerta) {
        return AlertaResponse.builder()
                .id(alerta.getId())
                .tipo(alerta.getTipo())
                .icono(icono(alerta.getTipo()))
                .titulo(alerta.getTitulo())
                .detalle(alerta.getDetalle())
                .tituloCompleto(alerta.getTitulo() + " - " + alerta.getDetalle())
                .leida(Boolean.TRUE.equals(alerta.getLeida()))
                .fechaCreacion(alerta.getFechaCreacion())
                .fechaLectura(alerta.getFechaLectura())
                .tiempoRelativo(formatearTiempo(alerta.getFechaCreacion()))
                .build();
    }

    private String icono(Alerta.TipoAlerta tipo) {
        return switch (tipo) {
            case STOCK_CRITICO, VENTAS_BAJAS -> "⚠️";
            case MARGEN_NORMALIZADO -> "✅";
            case INFORMACION -> "ℹ️";
        };
    }

    private String formatearTiempo(LocalDateTime fechaCreacion) {
        if (fechaCreacion == null) {
            return "hace un momento";
        }

        Duration duracion = Duration.between(fechaCreacion, LocalDateTime.now());
        long minutos = Math.max(0, duracion.toMinutes());

        if (minutos < 1) {
            return "hace un momento";
        }
        if (minutos < 60) {
            return "hace " + minutos + " min";
        }

        long horas = minutos / 60;
        if (horas < 24) {
            return "hace " + horas + " hora" + (horas == 1 ? "" : "s");
        }

        long dias = horas / 24;
        return "hace " + dias + " día" + (dias == 1 ? "" : "s");
    }
}
