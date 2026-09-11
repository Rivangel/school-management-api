package com.escuela.model.dto;

import java.util.List;

/**
 * Indicadores agregados para el dashboard: conteos, promedio de calificaciones
 * y porcentaje de asistencia, generales y por materia.
 *
 * @param promedioGeneral    {@code null} si no hay ninguna calificación registrada
 * @param asistenciaGeneral  resumen de asistencia de todo el centro
 */
public record EstadisticasResponse(
        long totalAlumnos,
        long totalMaestros,
        long totalMaterias,
        Double promedioGeneral,
        List<PromedioMateriaResponse> promedioPorMateria,
        AsistenciaResumenResponse asistenciaGeneral,
        List<AsistenciaMateriaResponse> asistenciaPorMateria
) {
}
