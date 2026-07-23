package com.escuela.model.dto;

import com.escuela.model.entity.Materia;

public record MateriaResponse(
        Long id,
        String nombre,
        Integer creditos,
        Long maestroId,
        String maestroNombre
) {

    public static MateriaResponse fromEntity(Materia materia) {
        return new MateriaResponse(
                materia.getId(),
                materia.getNombre(),
                materia.getCreditos(),
                materia.getMaestro().getId(),
                materia.getMaestro().getNombreCompleto()
        );
    }
}
