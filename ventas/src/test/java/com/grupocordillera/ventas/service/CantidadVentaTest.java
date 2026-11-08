package com.grupocordillera.ventas.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.grupocordillera.ventas.client.ClientesClient;
import com.grupocordillera.ventas.client.InventarioClient;
import com.grupocordillera.ventas.model.Venta;
import com.grupocordillera.ventas.repository.VentasRepository;

@ExtendWith(MockitoExtension.class)
class CantidadVentaTest {
    @Mock private VentasRepository repository;
    @Mock private InventarioClient inventario;
    @Mock private ClientesClient clientes;
    @Mock private FinanzasSyncService finanzas;
    @InjectMocks private VentasService service;

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void cantidadInvalidaSeRechazaAntesDeEfectos(Integer cantidad) {
        Venta venta = new Venta();
        venta.setCantidad(cantidad);

        assertThrows(IllegalArgumentException.class, () -> service.registrarVenta(venta));

        verifyNoInteractions(repository, inventario, clientes, finanzas);
    }
}
