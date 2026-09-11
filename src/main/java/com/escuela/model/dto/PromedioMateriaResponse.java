package com.escuela.model.dto;

/** Promedio de calificaciones de una materia. Solo incluye materias con al menos una nota. */
public record PromedioMateriaResponse(
        Long materiaId,
        String materiaNombre,
        Double promedio,
        long cantidadCalificaciones
) {
}
