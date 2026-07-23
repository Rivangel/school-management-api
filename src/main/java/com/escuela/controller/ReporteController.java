package com.escuela.controller;

import com.escuela.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Generación de reportes en PDF")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/boleta/{alumnoId}")
    @Operation(summary = "Descargar boleta de calificaciones en PDF",
            description = "Genera la boleta del alumno con sus calificaciones y promedio. "
                    + "Un ALUMNO solo puede descargar su propia boleta")
    public ResponseEntity<byte[]> descargarBoleta(@PathVariable Long alumnoId) {
        ReporteService.BoletaPdf boleta = reporteService.generarBoleta(alumnoId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + boleta.nombreArchivo() + "\"")
                .body(boleta.contenido());
    }
}
