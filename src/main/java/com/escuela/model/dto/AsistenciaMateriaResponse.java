package com.escuela.model.dto;

/** Asistencia de una materia. Solo incluye materias con al menos un registro. */
public record AsistenciaMateriaResponse(
        Long materiaId,
        String materiaNombre,
        long presentes,
        long total,
        Double porcentaje
) {
}
