package com.grupocordillera.kpis.service;

import com.grupocordillera.kpis.client.ClientesClient;
import com.grupocordillera.kpis.client.FinanzasClient;
import com.grupocordillera.kpis.client.InventarioClient;
import com.grupocordillera.kpis.client.VentasClient;
import com.grupocordillera.kpis.model.ClienteKpiDTO;
import com.grupocordillera.kpis.model.MovimientoFinancieroKpiDTO;
import com.grupocordillera.kpis.model.ProductoKpiDTO;
import com.grupocordillera.kpis.model.VentaKpiDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KpiDataService {

    private final VentasClient ventasClient;
    private final InventarioClient inventarioClient;
    private final FinanzasClient finanzasClient;
    private final ClientesClient clientesClient;

    @CircuitBreaker(name = "ventas", fallbackMethod = "fallbackVentas")
    public List<VentaKpiDTO> obtenerVentas() {
        return ventasClient.obtenerVentas();
    }

    public List<VentaKpiDTO> fallbackVentas(Throwable ignored) {
        return List.of();
    }

    @CircuitBreaker(name = "inventario", fallbackMethod = "fallbackProductos")
    public List<ProductoKpiDTO> obtenerProductos() {
        return inventarioClient.obtenerProductos();
    }

    public List<ProductoKpiDTO> fallbackProductos(Throwable ignored) {
        return List.of();
    }

    @CircuitBreaker(name = "finanzas", fallbackMethod = "fallbackMovimientos")
    public List<MovimientoFinancieroKpiDTO> obtenerMovimientos() {
        return finanzasClient.obtenerMovimientos();
    }

    public List<MovimientoFinancieroKpiDTO> fallbackMovimientos(Throwable ignored) {
        return List.of();
    }

    @CircuitBreaker(name = "clientes", fallbackMethod = "fallbackClientes")
    public List<ClienteKpiDTO> obtenerClientes() {
        return clientesClient.obtenerClientes();
    }

    public List<ClienteKpiDTO> fallbackClientes(Throwable ignored) {
        return List.of();
    }
}
