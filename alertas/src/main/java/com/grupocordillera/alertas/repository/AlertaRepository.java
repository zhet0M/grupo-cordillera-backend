package com.grupocordillera.alertas.repository;

import com.grupocordillera.alertas.model.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    Optional<Alerta> findByCodigo(String codigo);

    long countByLeidaFalse();

    List<Alerta> findByLeidaFalseOrderByFechaCreacionDesc();

    List<Alerta> findAllByOrderByFechaCreacionDesc();
}
