package com.grupocordillera.ventas.model;

import java.util.Locale;

public enum Sucursal {
    CENTRAL("SUC-CENTRAL"),
    NORTE("SUC-NORTE"),
    SUR("SUC-SUR"),
    OESTE("SUC-OESTE"),
    ESTE("SUC-ESTE");

    private final String valor;

    Sucursal(String valor) {
        this.valor = valor;
    }

    public String valor() {
        return valor;
    }

    public static Sucursal from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La sucursal es obligatoria");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (Sucursal sucursal : values()) {
            if (sucursal.valor.equals(normalized)) {
                return sucursal;
            }
        }

        throw new IllegalArgumentException("Sucursal no válida: " + value);
    }
}
