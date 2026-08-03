package com.escuela.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CalificacionRequest(

        @NotNull(message = "El id del alumno es obligatorio")
        @Positive(message = "El id del alumno debe ser un número positivo")
        Long alumnoId,

        @NotNull(message = "El id de la materia es obligatorio")
        @Positive(message = "El id de la materia debe ser un número positivo")
        Long materiaId,

        @NotNull(message = "La calificación es obligatoria")
        @DecimalMin(value = "0.0", message = "La calificación mínima es 0")
        @DecimalMax(value = "10.0", message = "La calificación máxima es 10")
        BigDecimal calificacion,

        @NotBlank(message = "El periodo es obligatorio (ej. 2026-1)")
        @Size(max = 20, message = "El periodo no puede exceder 20 caracteres")
        @Pattern(regexp = "\\d{4}-[12]", message = "El periodo debe tener el formato AAAA-S, con S 1 o 2 (ej. 2026-1)")
        String periodo
) {

    public CalificacionRequest {
        periodo = CamposDeTexto.limpiar(periodo);
    }
}
