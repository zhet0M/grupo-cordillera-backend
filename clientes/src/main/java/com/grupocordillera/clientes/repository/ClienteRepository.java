package com.grupocordillera.clientes.repository;

import com.grupocordillera.clientes.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Cliente> findByTipoCliente(Cliente.TipoCliente tipoCliente);

    List<Cliente> findByEstado(Cliente.Estado estado);
}
