package com.grupocordillera.finanzas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.grupocordillera.finanzas.client.InventarioClient;
import com.grupocordillera.finanzas.dto.MovimientoFinancieroRequest;
import com.grupocordillera.finanzas.dto.ProductoInventarioDTO;
import com.grupocordillera.finanzas.dto.ResumenFinancieroDTO;
import com.grupocordillera.finanzas.model.MovimientoFinanciero;
import com.grupocordillera.finanzas.repository.MovimientoFinancieroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinanzasServiceTest {

    @Mock
    private MovimientoFinancieroRepository repository;

    @Mock
    private InventarioClient inventarioClient;

    @InjectMocks
    private FinanzasService finanzasService;

    @BeforeEach
    void setUp() {
        lenient().when(repository.save(any(MovimientoFinanciero.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registrarMovimientoConVentaExistenteRetornaElExistente() {
        MovimientoFinanciero existente = movimientoBase();
        existente.setId(5L);
        existente.setVentaId(99L);
        when(repository.findByVentaId(99L)).thenReturn(Optional.of(existente));

        MovimientoFinancieroRequest request = new MovimientoFinancieroRequest();
        request.setVentaId(99L);

        MovimientoFinanciero resultado = finanzasService.registrarMovimiento(request);

        assertSame(existente, resultado);
        verifyNoInteractions(inventarioClient);
    }

    @Test
    void registrarMovimientoCalculaCostoYUsaSucursalDelInventario() {
        ProductoInventarioDTO producto = new ProductoInventarioDTO();
        producto.setId(7L);
        producto.setSku("TEC-001");
        producto.setNombre("Notebook");
        producto.setCosto(15.0);
        producto.setStock(10);
        producto.setSucursal("SUC-CENTRAL");

        when(inventarioClient.obtenerPorSku("TEC-001")).thenReturn(producto);

        MovimientoFinancieroRequest request = new MovimientoFinancieroRequest();
        request.setSkuProducto("TEC-001");
        request.setCantidad(2);
        request.setIngresos(BigDecimal.valueOf(100));
        request.setSucursal("suc-central");
        request.setTipoMovimiento(MovimientoFinanciero.TipoMovimiento.INGRESO);

        MovimientoFinanciero resultado = finanzasService.registrarMovimiento(request);

        assertEquals(7L, resultado.getProductoId());
        assertEquals("TEC-001", resultado.getSkuProducto());
        assertEquals("Notebook", resultado.getNombreProducto());
        assertEquals(BigDecimal.valueOf(100), resultado.getIngresos());
        assertEquals(BigDecimal.valueOf(30.0), resultado.getCosto());
        assertEquals(BigDecimal.valueOf(70.0), resultado.getMargen());
        assertEquals("SUC-CENTRAL", resultado.getSucursal());
    }

    @Test
    void obtenerTotalesPorSucursalNormalizaSucursal() {
        when(repository.sumIngresosBySucursal("SUC-OESTE")).thenReturn(BigDecimal.valueOf(1000));
        when(repository.sumCostosBySucursal("SUC-OESTE")).thenReturn(BigDecimal.valueOf(600));
        when(repository.sumMargenBySucursal("SUC-OESTE")).thenReturn(BigDecimal.valueOf(400));

        ResumenFinancieroDTO resultado = finanzasService.obtenerTotalesPorSucursal("suc-oeste");

        assertEquals(BigDecimal.valueOf(1000), resultado.getIngresos());
        assertEquals(BigDecimal.valueOf(600), resultado.getCosto());
        assertEquals(BigDecimal.valueOf(400), resultado.getMargen());
    }

    private MovimientoFinanciero movimientoBase() {
        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setIngresos(BigDecimal.ONE);
        movimiento.setCosto(BigDecimal.ONE);
        movimiento.setMargen(BigDecimal.ZERO);
        movimiento.setFechaRegistro(LocalDate.now());
        movimiento.setSucursal("SUC-CENTRAL");
        movimiento.setTipoMovimiento(MovimientoFinanciero.TipoMovimiento.INGRESO);
        return movimiento;
    }
}
