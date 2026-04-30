package com.grupocordillera.inventario.repository;

import com.grupocordillera.inventario.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findBySku(String sku);
    List<Producto> findBySucursal(String sucursal);
    List<Producto> findByCategoria(String categoria);
    List<Producto> findByEstado(Producto.Estado estado);
}
