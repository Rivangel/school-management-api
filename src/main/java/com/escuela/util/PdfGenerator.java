package com.escuela.util;

import com.escuela.model.entity.Alumno;
import com.escuela.model.entity.Calificacion;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera la boleta de calificaciones de un alumno en formato PDF usando iText.
 */
@Component
public class PdfGenerator {

    private static final DeviceRgb COLOR_ENCABEZADO = new DeviceRgb(25, 55, 109);
    private static final BigDecimal CALIFICACION_APROBATORIA = new BigDecimal("6.0");

    public byte[] generarBoleta(Alumno alumno, List<Calificacion> calificaciones) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try (PdfDocument pdf = new PdfDocument(new PdfWriter(salida));
             Document documento = new Document(pdf)) {

            documento.add(new Paragraph("Boleta de Calificaciones")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            documento.add(new Paragraph("Sistema de Gestión Escolar")
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginBottom(20));

            documento.add(datosDelAlumno(alumno));
            documento.add(new Paragraph("\n"));
            documento.add(tablaDeCalificaciones(calificaciones));
            documento.add(new Paragraph("\n"));
            documento.add(resumen(calificaciones));

            documento.add(new Paragraph("Generado el "
                    + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(30));
        }

        return salida.toByteArray();
    }

    private Table datosDelAlumno(Alumno alumno) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                .useAllAvailableWidth();

        agregarFilaDato(tabla, "Alumno", alumno.getNombreCompleto());
        agregarFilaDato(tabla, "Matrícula", alumno.getMatricula());
        agregarFilaDato(tabla, "Grupo", alumno.getGrupo());
        agregarFilaDato(tabla, "Email", alumno.getEmail());

        return tabla;
    }

    private void agregarFilaDato(Table tabla, String etiqueta, String valor) {
        tabla.addCell(new Cell()
                .add(new Paragraph(etiqueta).setBold())
                .setBorder(Border.NO_BORDER));
        tabla.addCell(new Cell()
                .add(new Paragraph(valor))
                .setBorder(Border.NO_BORDER));
    }

    private Table tablaDeCalificaciones(List<Calificacion> calificaciones) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1, 1}))
                .useAllAvailableWidth();

        for (String encabezado : new String[]{"Materia", "Créditos", "Periodo", "Calificación"}) {
            tabla.addHeaderCell(new Cell()
                    .add(new Paragraph(encabezado).setBold())
                    .setBackgroundColor(COLOR_ENCABEZADO)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        if (calificaciones.isEmpty()) {
            tabla.addCell(new Cell(1, 4)
                    .add(new Paragraph("Sin calificaciones registradas"))
                    .setTextAlignment(TextAlignment.CENTER));
            return tabla;
        }

        for (Calificacion calificacion : calificaciones) {
            tabla.addCell(new Cell().add(new Paragraph(calificacion.getMateria().getNombre())));
            tabla.addCell(new Cell()
                    .add(new Paragraph(String.valueOf(calificacion.getMateria().getCreditos())))
                    .setTextAlignment(TextAlignment.CENTER));
            tabla.addCell(new Cell()
                    .add(new Paragraph(calificacion.getPeriodo()))
                    .setTextAlignment(TextAlignment.CENTER));

            boolean aprobada = calificacion.getCalificacion()
                    .compareTo(CALIFICACION_APROBATORIA) >= 0;
            tabla.addCell(new Cell()
                    .add(new Paragraph(calificacion.getCalificacion().toPlainString()).setBold())
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(aprobada ? new DeviceRgb(0, 128, 0) : ColorConstants.RED));
        }

        return tabla;
    }

    private Paragraph resumen(List<Calificacion> calificaciones) {
        if (calificaciones.isEmpty()) {
            return new Paragraph("Promedio general: N/A").setBold();
        }

        BigDecimal suma = calificaciones.stream()
                .map(Calificacion::getCalificacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = suma.divide(
                BigDecimal.valueOf(calificaciones.size()), 2, RoundingMode.HALF_UP);

        return new Paragraph("Promedio general: " + promedio.toPlainString())
                .setBold()
                .setFontSize(13);
    }
}
