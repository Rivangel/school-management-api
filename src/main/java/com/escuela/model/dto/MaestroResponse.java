package com.escuela.model.dto;

import com.escuela.model.entity.Maestro;

public record MaestroResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        String especialidad
) {

    public static MaestroResponse fromEntity(Maestro maestro) {
        return new MaestroResponse(
                maestro.getId(),
                maestro.getNombre(),
                maestro.getApellido(),
                maestro.getEmail(),
                maestro.getEspecialidad()
        );
    }
}
