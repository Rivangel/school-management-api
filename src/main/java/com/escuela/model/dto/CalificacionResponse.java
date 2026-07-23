package com.escuela.model.dto;

import com.escuela.model.entity.Calificacion;

import java.math.BigDecimal;

public record CalificacionResponse(
        Long id,
        Long alumnoId,
        String alumnoNombre,
        Long materiaId,
        String materiaNombre,
        BigDecimal calificacion,
        String periodo
) {

    public static CalificacionResponse fromEntity(Calificacion c) {
        return new CalificacionResponse(
                c.getId(),
                c.getAlumno().getId(),
                c.getAlumno().getNombreCompleto(),
                c.getMateria().getId(),
                c.getMateria().getNombre(),
                c.getCalificacion(),
                c.getPeriodo()
        );
    }
}
