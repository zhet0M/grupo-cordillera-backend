package com.grupocordillera.inventario.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.grupocordillera.inventario.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class CantidadStockTest {
    @Mock private ProductoRepository repository;
    @InjectMocks private InventarioService service;

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void cantidadInvalidaNoConsultaNiModificaInventario(int cantidad) {
        assertThrows(IllegalArgumentException.class, () -> service.descontarStock("TEC-QA", cantidad));

        verifyNoInteractions(repository);
    }
}
