package com.grupocordillera.reportes.service;

import com.grupocordillera.reportes.client.FinanzasClient;
import com.grupocordillera.reportes.client.InventarioClient;
import com.grupocordillera.reportes.client.VentasClient;
import com.grupocordillera.reportes.dto.MovimientoFinancieroReporteDTO;
import com.grupocordillera.reportes.dto.ProductoReporteDTO;
import com.grupocordillera.reportes.dto.ResumenFinancieroDTO;
import com.grupocordillera.reportes.dto.VentaReporteDTO;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FOOTER_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int BARRAS_GRAFICO = 20;

    private final VentasClient ventasClient;
    private final InventarioClient inventarioClient;
    private final FinanzasClient finanzasClient;

    public byte[] generarReporteVentas(LocalDate inicio, LocalDate fin) {
        RangoFechas rango = resolverRango(inicio, fin);
        List<VentaReporteDTO> ventas = ventasClient.obtenerPorRango(rango.inicio(), rango.fin());
        BigDecimal total = ventas.stream()
                .map(v -> BigDecimal.valueOf(v.getMontoTotal() == null ? 0d : v.getMontoTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return crearPdf("Reporte de Ventas", "Periodo: " + rango.descripcion(), document -> {
            agregarResumen(document,
                    "Ventas registradas", String.valueOf(ventas.size()),
                    "Monto total", formatearMoneda(total),
                    "Sucursales", String.valueOf(ventas.stream().map(VentaReporteDTO::getSucursal).distinct().count()));

            document.add(new LineSeparator(new SolidLine(1f)).setMarginBottom(12));

            Table table = crearTabla(6, "Fecha", "Sucursal", "Producto", "Cantidad", "Total", "Estado finanzas");
            ventas.stream()
                    .sorted(Comparator.comparing(VentaReporteDTO::getFecha, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .forEach(venta -> {
                        table.addCell(celda(venta.getFecha() == null ? "-" : venta.getFecha().format(FORMATO_FECHA)));
                        table.addCell(celda(texto(venta.getSucursal())));
                        table.addCell(celda(texto(venta.getNombreProducto())));
                        table.addCell(celda(texto(venta.getCantidad())));
                        table.addCell(celda(formatearMoneda(BigDecimal.valueOf(venta.getMontoTotal() == null ? 0d : venta.getMontoTotal()))));
                        table.addCell(celdaEstadoFinanzas(texto(venta.getEstadoFinanzas())));
                    });
            document.add(table);
        });
    }

    public byte[] generarReporteInventario() {
        List<ProductoReporteDTO> productos = inventarioClient.obtenerTodos();
        long criticos = productos.stream().filter(this::esCritico).count();
        long agotados = productos.stream().filter(p -> "AGOTADO".equalsIgnoreCase(texto(p.getEstado()))).count();

        return crearPdf("Reporte de Inventario", "Inventario general del sistema", document -> {
            agregarResumen(document,
                    "Productos", String.valueOf(productos.size()),
                    "Críticos", String.valueOf(criticos),
                    "Agotados", String.valueOf(agotados));

            Table table = crearTabla(6, "SKU", "Nombre", "Sucursal", "Stock", "Mínimo", "Estado");
            productos.stream()
                    .sorted(Comparator.comparing(ProductoReporteDTO::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .forEach(producto -> {
                        boolean critico = esCritico(producto);
                        Color rowBackground = critico ? new com.itextpdf.kernel.colors.DeviceRgb(255, 235, 235) : null;
                        Color rowFont = critico ? new com.itextpdf.kernel.colors.DeviceRgb(178, 34, 34) : null;

                        table.addCell(celda(texto(producto.getSku()), rowBackground, rowFont, critico));
                        table.addCell(celda(texto(producto.getNombre()), rowBackground, rowFont, critico));
                        table.addCell(celda(texto(producto.getSucursal()), rowBackground, rowFont, critico));
                        table.addCell(celda(texto(producto.getStock()), rowBackground, rowFont, critico));
                        table.addCell(celda(texto(producto.getStockMinimo()), rowBackground, rowFont, critico));
                        table.addCell(celda(texto(producto.getEstado()), rowBackground, rowFont, critico));
                    });
            document.add(table);
        });
    }

    public byte[] generarReporteFinanzas(LocalDate inicio, LocalDate fin) {
        RangoFechas rango = resolverRango(inicio, fin);
        List<MovimientoFinancieroReporteDTO> movimientos = finanzasClient.obtenerPorRango(rango.inicio(), rango.fin());
        ResumenFinancieroDTO resumen = finanzasClient.obtenerTotalesPorRango(rango.inicio(), rango.fin());
        BigDecimal ingresos = safe(resumen.getIngresos());
        BigDecimal costo = safe(resumen.getCosto());
        BigDecimal margen = safe(resumen.getMargen());
        BigDecimal rentabilidad = ingresos.compareTo(BigDecimal.ZERO) > 0
                ? margen.multiply(BigDecimal.valueOf(100)).divide(ingresos, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return crearPdf("Reporte de Finanzas", "Periodo: " + rango.descripcion(), document -> {
            document.add(new Paragraph("Período: " + rango.descripcion())
                    .setFontSize(11)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setMarginBottom(8));

            document.add(new LineSeparator(new SolidLine(1f)).setMarginBottom(14));

            agregarResumenCuatro(document,
                    "Ingresos", formatearMoneda(ingresos),
                    "Costos", formatearMoneda(costo),
                    "Margen", formatearMoneda(margen),
                    "Rentabilidad", formatearPorcentaje(rentabilidad));

            document.add(new Paragraph("Tabla de movimientos")
                    .setFontSize(12)
                    .setBold()
                    .setMarginTop(8)
                    .setMarginBottom(8));

            document.add(new LineSeparator(new SolidLine(1f)).setMarginBottom(12));

            Table table = crearTabla(6, "Fecha", "Sucursal", "Ingresos", "Costos", "Margen", "Utilidad");
            movimientos.stream()
                    .sorted(Comparator.comparing(MovimientoFinancieroReporteDTO::getFechaRegistro, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .forEach(movimiento -> {
                        table.addCell(celda(movimiento.getFechaRegistro() == null ? "-" : movimiento.getFechaRegistro().format(FORMATO_FECHA)));
                        table.addCell(celda(texto(movimiento.getSucursal())));
                        table.addCell(celda(formatearMoneda(movimiento.getIngresos())));
                        table.addCell(celda(formatearMoneda(movimiento.getCosto())));
                        table.addCell(celda(formatearMoneda(movimiento.getMargen())));
                        table.addCell(celda(formatearMoneda(movimiento.getMargen())));
                    });
            document.add(table);

            document.add(new Paragraph("Totales")
                    .setFontSize(12)
                    .setBold()
                    .setMarginTop(16)
                    .setMarginBottom(8));
            document.add(new LineSeparator(new SolidLine(1f)).setMarginBottom(12));

            Table totales = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1})).useAllAvailableWidth();
            totales.addCell(resumenCell("Ingresos", formatearMoneda(ingresos)));
            totales.addCell(resumenCell("Costos", formatearMoneda(costo)));
            totales.addCell(resumenCell("Margen", formatearMoneda(margen)));
            totales.addCell(resumenCell("Rentabilidad", formatearPorcentaje(rentabilidad)));
            document.add(totales);

            document.add(new Paragraph("Gráfico de barras")
                    .setFontSize(12)
                    .setBold()
                    .setMarginTop(16)
                    .setMarginBottom(8));
            document.add(new LineSeparator(new SolidLine(1f)).setMarginBottom(12));
            document.add(crearGraficoBarras(ingresos, costo, margen));
        });
    }

    private byte[] crearPdf(String titulo, String subtitulo, PdfBodyBuilder bodyBuilder) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            FooterEventHandler footerEventHandler = new FooterEventHandler();
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, footerEventHandler);

            Document document = new Document(pdf, PageSize.A4.rotate());
            document.setMargins(28, 28, 42, 28);

            document.add(new Paragraph(titulo)
                    .setFontSize(20)
                    .setBold()
                    .setFontColor(ColorConstants.BLACK)
                    .setMarginBottom(4));

            document.add(new Paragraph(subtitulo)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setMarginBottom(16));

            bodyBuilder.build(document);
            footerEventHandler.writeTotalPages(pdf);
            document.close();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte.", ex);
        }
    }

    private void agregarResumen(Document document, String etiqueta1, String valor1, String etiqueta2, String valor2, String etiqueta3, String valor3) {
        Table resumen = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1})).useAllAvailableWidth();
        resumen.addCell(resumenCell(etiqueta1, valor1));
        resumen.addCell(resumenCell(etiqueta2, valor2));
        resumen.addCell(resumenCell(etiqueta3, valor3));
        resumen.setMarginBottom(16);
        document.add(resumen);
    }

    private Cell resumenCell(String etiqueta, String valor) {
        return new Cell()
                .setPadding(12)
                .setBackgroundColor(ColorConstants.WHITE)
                .add(new Paragraph(etiqueta).setFontSize(9).setFontColor(ColorConstants.DARK_GRAY).setMarginBottom(4))
                .add(new Paragraph(valor).setFontSize(16).setBold())
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private void agregarResumenCuatro(Document document,
                                      String etiqueta1, String valor1,
                                      String etiqueta2, String valor2,
                                      String etiqueta3, String valor3,
                                      String etiqueta4, String valor4) {
        Table resumen = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1})).useAllAvailableWidth();
        resumen.addCell(resumenCell(etiqueta1, valor1));
        resumen.addCell(resumenCell(etiqueta2, valor2));
        resumen.addCell(resumenCell(etiqueta3, valor3));
        resumen.addCell(resumenCell(etiqueta4, valor4));
        resumen.setMarginBottom(16);
        document.add(resumen);
    }

    private Table crearGraficoBarras(BigDecimal ingresos, BigDecimal costo, BigDecimal margen) {
        Table graph = new Table(UnitValue.createPercentArray(new float[]{1, 6})).useAllAvailableWidth();
        graph.addCell(etiquetaGrafico("Ingresos"));
        graph.addCell(barraGrafico(ingresos, maximoGrafico(ingresos, costo, margen), new com.itextpdf.kernel.colors.DeviceRgb(30, 214, 188)));
        graph.addCell(etiquetaGrafico("Costos"));
        graph.addCell(barraGrafico(costo, maximoGrafico(ingresos, costo, margen), new com.itextpdf.kernel.colors.DeviceRgb(255, 157, 90)));
        graph.addCell(etiquetaGrafico("Margen"));
        graph.addCell(barraGrafico(margen, maximoGrafico(ingresos, costo, margen), new com.itextpdf.kernel.colors.DeviceRgb(63, 102, 245)));
        return graph;
    }

    private Cell etiquetaGrafico(String texto) {
        return new Cell()
                .setBorder(null)
                .setPadding(6)
                .add(new Paragraph(texto).setFontSize(9).setBold());
    }

    private Cell barraGrafico(BigDecimal valor, BigDecimal maximo, Color color) {
        BigDecimal base = maximo.compareTo(BigDecimal.ZERO) > 0 ? valor.abs().multiply(BigDecimal.valueOf(BARRAS_GRAFICO)).divide(maximo, 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        int bloques = Math.min(BARRAS_GRAFICO, Math.max(0, base.intValue()));
        Table barra = new Table(UnitValue.createPercentArray(BARRAS_GRAFICO)).useAllAvailableWidth();
        for (int i = 0; i < BARRAS_GRAFICO; i++) {
            Cell bloque = new Cell().setHeight(12).setBorder(null);
            bloque.setBackgroundColor(i < bloques ? color : new com.itextpdf.kernel.colors.DeviceRgb(230, 235, 242));
            barra.addCell(bloque);
        }
        return new Cell().setBorder(null).setPadding(4).add(barra);
    }

    private BigDecimal maximoGrafico(BigDecimal ingresos, BigDecimal costo, BigDecimal margen) {
        return ingresos.abs().max(costo.abs()).max(margen.abs());
    }

    private Cell celdaEstadoFinanzas(String texto) {
        boolean sincronizado = "SINCRONIZADO".equalsIgnoreCase(texto);
        boolean error = "ERROR".equalsIgnoreCase(texto);
        Color fondo = sincronizado
                ? new com.itextpdf.kernel.colors.DeviceRgb(229, 247, 239)
                : error
                ? new com.itextpdf.kernel.colors.DeviceRgb(255, 232, 232)
                : new com.itextpdf.kernel.colors.DeviceRgb(236, 240, 244);
        Color color = sincronizado
                ? new com.itextpdf.kernel.colors.DeviceRgb(19, 128, 83)
                : error
                ? new com.itextpdf.kernel.colors.DeviceRgb(179, 38, 30)
                : ColorConstants.DARK_GRAY;
        return celda(texto, fondo, color, true);
    }

    private Table crearTabla(int columnas, String... headers) {
        float[] widths = new float[columnas];
        Arrays.fill(widths, 1f);
        Table table = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(ColorConstants.BLACK)
                    .setPadding(8)
                    .add(new Paragraph(header)
                            .setFontSize(10)
                            .setFontColor(ColorConstants.WHITE)
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER)));
        }
        return table;
    }

    private Cell celda(String texto) {
        return celda(texto, null, null, false);
    }

    private Cell celda(String texto, Color backgroundColor, Color fontColor, boolean bold) {
        Cell cell = new Cell()
                .setPadding(8)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.LEFT);
        if (backgroundColor != null) {
            cell.setBackgroundColor(backgroundColor);
        }

        Paragraph paragraph = new Paragraph(texto == null || texto.isBlank() ? "-" : texto).setFontSize(9);
        if (fontColor != null) {
            paragraph.setFontColor(fontColor);
        }
        if (bold) {
            paragraph.setBold();
        }
        return cell.add(paragraph);
    }

    private String texto(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, ProductoReporteDTO> cargarProductosPorSku() {
        Map<String, ProductoReporteDTO> productosPorSku = new HashMap<>();
        for (ProductoReporteDTO producto : inventarioClient.obtenerTodos()) {
            if (producto != null && producto.getSku() != null) {
                productosPorSku.put(producto.getSku(), producto);
            }
        }
        return productosPorSku;
    }

    private boolean esCritico(ProductoReporteDTO producto) {
        if (producto == null || producto.getStock() == null || producto.getStockMinimo() == null) {
            return false;
        }
        return producto.getStock() <= producto.getStockMinimo() && !"DESCONTINUADO".equalsIgnoreCase(texto(producto.getEstado()));
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatearMoneda(BigDecimal valor) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
        return format.format(valor == null ? BigDecimal.ZERO : valor);
    }

    private String formatearPorcentaje(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP) + "%";
    }

    private RangoFechas resolverRango(LocalDate inicio, LocalDate fin) {
        LocalDate fechaFin = fin != null ? fin : LocalDate.now();
        LocalDate fechaInicio = inicio != null ? inicio : YearMonth.from(fechaFin).atDay(1);

        if (fechaInicio.isAfter(fechaFin)) {
            LocalDate temp = fechaInicio;
            fechaInicio = fechaFin;
            fechaFin = temp;
        }

        return new RangoFechas(fechaInicio, fechaFin);
    }

    private record RangoFechas(LocalDate inicio, LocalDate fin) {
        String descripcion() {
            return inicio.format(FORMATO_FECHA) + " - " + fin.format(FORMATO_FECHA);
        }
    }

    private final class FooterEventHandler implements IEventHandler {
        private final PdfFormXObject totalPagesPlaceholder = new PdfFormXObject(new Rectangle(0, 0, 60, 12));
        private final String fechaGeneracion = LocalDate.now().format(FORMATO_FOOTER_FECHA);
        private final PdfFont footerFont;

        private FooterEventHandler() throws java.io.IOException {
            this.footerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            var page = docEvent.getPage();
            Rectangle pageSize = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdf);
            Canvas canvas = new Canvas(pdfCanvas, pageSize);

            String prefix = "Generado el: " + fechaGeneracion + " | Sistema de Monitoreo Grupo Cordillera | Página " + pdf.getPageNumber(page) + " de ";
            float x = pageSize.getLeft() + 28;
            float y = pageSize.getBottom() + 18;
            canvas.showTextAligned(new Paragraph(prefix).setFont(footerFont).setFontSize(8).setFontColor(ColorConstants.DARK_GRAY), x, y, TextAlignment.LEFT);
            float placeholderX = x + footerFont.getWidth(prefix, 8);
            pdfCanvas.addXObjectAt(totalPagesPlaceholder, placeholderX, y - 2);
            canvas.close();
            pdfCanvas.release();
        }

        private void writeTotalPages(PdfDocument pdf) throws java.io.IOException {
            PdfCanvas canvas = new PdfCanvas(totalPagesPlaceholder, pdf);
            canvas.beginText();
            canvas.setFontAndSize(footerFont, 8);
            canvas.moveText(0, 0);
            canvas.showText(String.valueOf(pdf.getNumberOfPages()));
            canvas.endText();
            canvas.release();
        }
    }

    @FunctionalInterface
    private interface PdfBodyBuilder {
        void build(Document document);
    }
}
