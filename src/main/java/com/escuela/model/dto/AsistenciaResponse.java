package com.escuela.model.dto;

import com.escuela.model.entity.Asistencia;

import java.time.LocalDate;

public record AsistenciaResponse(
        Long id,
        Long alumnoId,
        String alumnoNombre,
        Long materiaId,
        String materiaNombre,
        LocalDate fecha,
        Boolean presente
) {

    public static AsistenciaResponse fromEntity(Asistencia a) {
        return new AsistenciaResponse(
                a.getId(),
                a.getAlumno().getId(),
                a.getAlumno().getNombreCompleto(),
                a.getMateria().getId(),
                a.getMateria().getNombre(),
                a.getFecha(),
                a.getPresente()
        );
    }
}
