package com.grupocordillera.ventas.repository;

import com.grupocordillera.ventas.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Collection;

public interface VentasRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByFecha(LocalDate fecha);

    List<Venta> findBySucursal(String sucursal);

    List<Venta> findByFechaBetween(LocalDate inicio, LocalDate fin);

    List<Venta> findByCanal(Venta.Canal canal);

    List<Venta> findByEstadoFinanzasIn(Collection<Venta.EstadoFinanzas> estados);

    // Total de ventas por un periodo de tiempo
    @Query("SELECT SUM(v.montoTotal) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin")
    Double sumMontoTotalByFechaBetween(LocalDate inicio, LocalDate fin);

    // Total de ventas por sucursal
    @Query("SELECT SUM(v.montoTotal) FROM Venta v WHERE v.sucursal = :sucursal")
    Double sumMontoTotalBySucursal(String sucursal);
}
