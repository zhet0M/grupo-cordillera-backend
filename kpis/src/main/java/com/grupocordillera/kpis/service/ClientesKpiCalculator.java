package com.grupocordillera.kpis.service;

import com.grupocordillera.kpis.dto.KpiResult;
import com.grupocordillera.kpis.enums.KpiEstado;
import com.grupocordillera.kpis.enums.KpiType;
import com.grupocordillera.kpis.model.ClienteKpiDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ClientesKpiCalculator implements KpiCalculator {

    private static final DateTimeFormatter MES_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final KpiDataService dataService;

    @Override
    public boolean supports(KpiType tipo) {
        return switch (tipo) {
            case CLIENTES_NUEVOS, CLIENTES_FRECUENTES -> true;
            default -> false;
        };
    }

    @Override
    public KpiResult calcular(KpiType tipo) {
        List<ClienteKpiDTO> clientes = dataService.obtenerClientes();
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        return switch (tipo) {
            case CLIENTES_NUEVOS -> {
                long nuevos = clientes.stream()
                        .filter(cliente -> cliente.getFechaRegistro() != null)
                        .filter(cliente -> !cliente.getFechaRegistro().isBefore(inicioMes) && !cliente.getFechaRegistro().isAfter(hoy))
                        .count();
                yield base(tipo, BigDecimal.valueOf(nuevos), "Clientes nuevos del periodo", "Clientes", periodo(formatearMes(inicioMes)), nuevos > 0 ? KpiEstado.POSITIVO : KpiEstado.SIN_DATOS);
            }
            case CLIENTES_FRECUENTES -> {
                long frecuentes = clientes.stream()
                        .filter(cliente -> cliente.getCantidadCompras() != null && cliente.getCantidadCompras() > 1)
                        .count();
                long nuevos = clientes.stream()
                        .filter(cliente -> cliente.getFechaRegistro() != null)
                        .filter(cliente -> !cliente.getFechaRegistro().isBefore(inicioMes) && !cliente.getFechaRegistro().isAfter(hoy))
                        .count();
                KpiResult result = base(tipo, BigDecimal.valueOf(frecuentes), "Clientes frecuentes vs nuevos", "Clientes", periodo(formatearMes(inicioMes)), frecuentes > 0 ? KpiEstado.POSITIVO : KpiEstado.SIN_DATOS);
                result.getDetalles().put("FRECUENTES", BigDecimal.valueOf(frecuentes));
                result.getDetalles().put("NUEVOS", BigDecimal.valueOf(nuevos));
                yield result;
            }
            default -> throw new IllegalArgumentException("KPI de clientes no soportado: " + tipo);
        };
    }

    private KpiResult base(KpiType tipo, BigDecimal valor, String descripcion, String fuenteDatos, String periodo, KpiEstado estado) {
        KpiResult result = new KpiResult();
        result.setTipo(tipo);
        result.setValor(valor);
        result.setVariacion(BigDecimal.ZERO);
        result.setDescripcion(descripcion);
        result.setFuenteDatos(fuenteDatos);
        result.setPeriodo(periodo);
        result.setEstado(estado);
        return result;
    }

    private String formatearMes(LocalDate fecha) {
        return YearMonth.from(fecha).atDay(1).format(MES_FORMAT);
    }

    private String periodo(String valor) {
        return valor;
    }
}
