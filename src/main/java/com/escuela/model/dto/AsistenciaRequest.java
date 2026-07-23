package com.escuela.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record AsistenciaRequest(

        @NotNull(message = "El id del alumno es obligatorio")
        Long alumnoId,

        @NotNull(message = "El id de la materia es obligatorio")
        Long materiaId,

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate fecha,

        @NotNull(message = "El campo presente es obligatorio")
        Boolean presente
) {
}
