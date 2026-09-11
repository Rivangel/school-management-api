package com.escuela.controller;

import com.escuela.model.dto.EstadisticasResponse;
import com.escuela.service.EstadisticasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estadisticas")
@Tag(name = "Estadísticas", description = "Indicadores agregados para el dashboard")
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    public EstadisticasController(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    @GetMapping
    @Operation(summary = "Obtener estadísticas del centro",
            description = "Conteos de alumnos/maestros/materias, promedio de calificaciones y "
                    + "porcentaje de asistencia, generales y por materia. Reservado a ADMIN y "
                    + "MAESTRO, igual que el resto de las consultas del grupo completo")
    public EstadisticasResponse obtener() {
        return estadisticasService.obtener();
    }
}
