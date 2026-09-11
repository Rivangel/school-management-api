package com.escuela.model.dto;

/**
 * Resumen de asistencia: cuántos registros hay y qué porcentaje estuvo presente.
 *
 * @param porcentaje {@code null} cuando {@code total} es 0: sin registros no hay porcentaje
 *                   que calcular, y un 0% diría que se faltó a todo.
 */
public record AsistenciaResumenResponse(
        long presentes,
        long ausentes,
        long total,
        Double porcentaje
) {
}
