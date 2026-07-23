package com.escuela.model.dto;

import com.escuela.model.entity.Alumno;

public record AlumnoResponse(
        Long id,
        String nombre,
        String apellido,
        String matricula,
        String email,
        String grupo
) {

    public static AlumnoResponse fromEntity(Alumno alumno) {
        return new AlumnoResponse(
                alumno.getId(),
                alumno.getNombre(),
                alumno.getApellido(),
                alumno.getMatricula(),
                alumno.getEmail(),
                alumno.getGrupo()
        );
    }
}
