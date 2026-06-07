package com.grupocordillera.kpis.enums;

import java.util.Locale;

public enum KpiType {
    VENTAS_DIA,
    VENTAS_SEMANA,
    VENTAS_MES,
    VENTAS_POR_SUCURSAL,
    VENTAS_POR_CANAL,
    TICKET_PROMEDIO,
    VARIACION_MENSUAL,
    SUCURSAL_MEJOR_RENDIMIENTO,
    STOCK_BAJO_MINIMO,
    ROTACION_INVENTARIO,
    INVENTARIO_TOTAL_VALOR,
    INGRESOS_TOTALES,
    MARGEN_RENTABILIDAD,
    COSTOS_OPERACIONALES,
    UTILIDAD_NETA,
    CLIENTES_NUEVOS,
    CLIENTES_FRECUENTES;

    public static KpiType from(String value) {
        return KpiType.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
