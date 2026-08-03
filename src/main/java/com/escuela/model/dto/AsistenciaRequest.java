package com.escuela.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record AsistenciaRequest(

        @NotNull(message = "El id del alumno es obligatorio")
        @Positive(message = "El id del alumno debe ser un número positivo")
        Long alumnoId,

        @NotNull(message = "El id de la materia es obligatorio")
        @Positive(message = "El id de la materia debe ser un número positivo")
        Long materiaId,

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate fecha,

        @NotNull(message = "El campo presente es obligatorio")
        Boolean presente
) {
}
