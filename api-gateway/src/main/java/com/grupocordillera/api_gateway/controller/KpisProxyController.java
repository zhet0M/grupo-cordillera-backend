package com.grupocordillera.api_gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/kpis")
public class KpisProxyController {

    private final RestTemplate restTemplate;
    private final String kpisUrl;

    public KpisProxyController(
            RestTemplate restTemplate,
            @Value("${kpis.url:http://ms_kpis:8086}") String kpisUrl) {
        this.restTemplate = restTemplate;
        this.kpisUrl = kpisUrl;
    }

    @GetMapping
    public ResponseEntity<String> obtenerResumen() {
        String response = restTemplate.getForObject(kpisUrl + "/kpis", String.class);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tipo}")
    public ResponseEntity<String> obtenerPorTipo(@PathVariable String tipo) {
        String response = restTemplate.getForObject(kpisUrl + "/kpis/" + tipo, String.class);
        return ResponseEntity.ok(response);
    }
}
