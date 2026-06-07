package com.grupocordillera.kpis.client;

import com.grupocordillera.kpis.model.ClienteKpiDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "clientes", url = "${app.services.clientes-url}")
public interface ClientesClient {

    @GetMapping
    List<ClienteKpiDTO> obtenerClientes();
}
