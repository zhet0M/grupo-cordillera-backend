package com.grupocordillera.finanzas.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.grupocordillera.finanzas.model.MovimientoFinanciero;

public interface MovimientoFinancieroRepository extends JpaRepository<MovimientoFinanciero, Long> {

    Optional<MovimientoFinanciero> findByVentaId(Long ventaId);

    List<MovimientoFinanciero> findByFechaRegistro(LocalDate fechaRegistro);

    List<MovimientoFinanciero> findBySucursalIgnoreCase(String sucursal);

    List<MovimientoFinanciero> findByTipoMovimiento(MovimientoFinanciero.TipoMovimiento tipoMovimiento);

    List<MovimientoFinanciero> findByFechaRegistroBetween(LocalDate inicio, LocalDate fin);

    @Query("SELECT COALESCE(SUM(m.ingresos), 0) FROM MovimientoFinanciero m")
    BigDecimal sumIngresos();

    @Query("SELECT COALESCE(SUM(m.costo), 0) FROM MovimientoFinanciero m")
    BigDecimal sumCostos();

    @Query("SELECT COALESCE(SUM(m.margen), 0) FROM MovimientoFinanciero m")
    BigDecimal sumMargen();

    @Query("SELECT COALESCE(SUM(m.ingresos), 0) FROM MovimientoFinanciero m WHERE m.fechaRegistro BETWEEN :inicio AND :fin")
    BigDecimal sumIngresosByFechaRegistroBetween(LocalDate inicio, LocalDate fin);

    @Query("SELECT COALESCE(SUM(m.costo), 0) FROM MovimientoFinanciero m WHERE m.fechaRegistro BETWEEN :inicio AND :fin")
    BigDecimal sumCostosByFechaRegistroBetween(LocalDate inicio, LocalDate fin);

    @Query("SELECT COALESCE(SUM(m.margen), 0) FROM MovimientoFinanciero m WHERE m.fechaRegistro BETWEEN :inicio AND :fin")
    BigDecimal sumMargenByFechaRegistroBetween(LocalDate inicio, LocalDate fin);

    @Query("SELECT COALESCE(SUM(m.ingresos), 0) FROM MovimientoFinanciero m WHERE LOWER(m.sucursal) = LOWER(:sucursal)")
    BigDecimal sumIngresosBySucursal(String sucursal);

    @Query("SELECT COALESCE(SUM(m.costo), 0) FROM MovimientoFinanciero m WHERE LOWER(m.sucursal) = LOWER(:sucursal)")
    BigDecimal sumCostosBySucursal(String sucursal);

    @Query("SELECT COALESCE(SUM(m.margen), 0) FROM MovimientoFinanciero m WHERE LOWER(m.sucursal) = LOWER(:sucursal)")
    BigDecimal sumMargenBySucursal(String sucursal);
}
