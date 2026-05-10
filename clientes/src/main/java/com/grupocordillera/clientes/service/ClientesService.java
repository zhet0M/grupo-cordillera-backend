package com.grupocordillera.clientes.service;

import com.grupocordillera.clientes.dto.ClienteRequest;
import com.grupocordillera.clientes.dto.RegistrarCompraRequest;
import com.grupocordillera.clientes.model.Cliente;
import com.grupocordillera.clientes.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientesService {

    private final ClienteRepository clienteRepository;

    public Cliente crearCliente(ClienteRequest request) {
        validarEmailDisponible(request.getEmail(), null);

        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setEmail(request.getEmail().trim().toLowerCase());
        cliente.setTelefono(request.getTelefono());
        cliente.setDireccion(request.getDireccion());
        cliente.setTipoCliente(normalizarTipoInicial(request.getTipoCliente()));
        cliente.setEstado(request.getEstado() != null ? request.getEstado() : Cliente.Estado.ACTIVO);
        cliente.setFechaRegistro(LocalDate.now());
        cliente.setCantidadCompras(0);
        cliente.setMontoAcumulado(BigDecimal.ZERO);
        cliente.setUltimaFechaCompra(null);
        return clienteRepository.save(cliente);
    }

    public Cliente actualizarCliente(Long id, ClienteRequest request) {
        Cliente cliente = obtenerPorId(id);
        validarEmailDisponible(request.getEmail(), id);

        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setEmail(request.getEmail().trim().toLowerCase());
        cliente.setTelefono(request.getTelefono());
        cliente.setDireccion(request.getDireccion());
        cliente.setEstado(request.getEstado() != null ? request.getEstado() : cliente.getEstado());
        if (request.getTipoCliente() != null) {
            cliente.setTipoCliente(request.getTipoCliente());
        }
        return clienteRepository.save(cliente);
    }

    public Cliente registrarCompra(Long id, RegistrarCompraRequest request) {
        Cliente cliente = obtenerPorId(id);
        if (cliente.getEstado() != Cliente.Estado.ACTIVO) {
            throw new RuntimeException("El cliente no esta activo para registrar compras");
        }

        LocalDate fechaCompra = request.getFechaCompra() != null ? request.getFechaCompra() : LocalDate.now();
        LocalDate fechaCompraAnterior = cliente.getUltimaFechaCompra();

        cliente.setCantidadCompras(cliente.getCantidadCompras() + 1);
        cliente.setMontoAcumulado(cliente.getMontoAcumulado().add(request.getMontoCompra()));
        cliente.setUltimaFechaCompra(fechaCompra);

        if (!esTipoManual(cliente.getTipoCliente())) {
            cliente.setTipoCliente(recalcularTipoCliente(cliente, fechaCompraAnterior, fechaCompra));
        }

        return clienteRepository.save(cliente);
    }

    public List<Cliente> obtenerTodos() {
        return clienteRepository.findAll();
    }

    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con id: " + id));
    }

    public List<Cliente> obtenerPorTipo(Cliente.TipoCliente tipoCliente) {
        return clienteRepository.findByTipoCliente(tipoCliente);
    }

    public List<Cliente> obtenerPorEstado(Cliente.Estado estado) {
        return clienteRepository.findByEstado(estado);
    }

    private void validarEmailDisponible(String email, Long clienteIdActual) {
        if (email == null || email.isBlank()) {
            return;
        }

        clienteRepository.findByEmailIgnoreCase(email.trim())
                .ifPresent(clienteExistente -> {
                    if (clienteIdActual == null || !clienteExistente.getId().equals(clienteIdActual)) {
                        throw new RuntimeException("Ya existe un cliente registrado con ese email");
                    }
                });
    }

    private Cliente.TipoCliente normalizarTipoInicial(Cliente.TipoCliente tipoCliente) {
        return tipoCliente != null ? tipoCliente : Cliente.TipoCliente.REGULAR;
    }

    private boolean esTipoManual(Cliente.TipoCliente tipoCliente) {
        return tipoCliente == Cliente.TipoCliente.CORPORATIVO || tipoCliente == Cliente.TipoCliente.MAYORISTA;
    }

    private Cliente.TipoCliente recalcularTipoCliente(Cliente cliente, LocalDate fechaCompraAnterior, LocalDate fechaCompraActual) {
        if (cliente.getCantidadCompras() >= 8 && cliente.getMontoAcumulado().compareTo(new BigDecimal("1500000")) >= 0) {
            return Cliente.TipoCliente.VIP;
        }

        boolean compraFrecuente = fechaCompraAnterior != null
                && ChronoUnit.DAYS.between(fechaCompraAnterior, fechaCompraActual) <= 45;

        if (cliente.getCantidadCompras() >= 3
                || cliente.getMontoAcumulado().compareTo(new BigDecimal("500000")) >= 0
                || compraFrecuente) {
            return Cliente.TipoCliente.FRECUENTE;
        }

        return Cliente.TipoCliente.REGULAR;
    }
}
