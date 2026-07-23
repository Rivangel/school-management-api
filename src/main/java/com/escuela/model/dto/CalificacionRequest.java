package com.escuela.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CalificacionRequest(

        @NotNull(message = "El id del alumno es obligatorio")
        Long alumnoId,

        @NotNull(message = "El id de la materia es obligatorio")
        Long materiaId,

        @NotNull(message = "La calificación es obligatoria")
        @DecimalMin(value = "0.0", message = "La calificación mínima es 0")
        @DecimalMax(value = "10.0", message = "La calificación máxima es 10")
        BigDecimal calificacion,

        @NotBlank(message = "El periodo es obligatorio (ej. 2026-1)")
        @Size(max = 20, message = "El periodo no puede exceder 20 caracteres")
        String periodo
) {
}
