package com.escuela.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaestroRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
        String apellido,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        @Size(max = 120, message = "El email no puede exceder 120 caracteres")
        String email,

        @NotBlank(message = "La especialidad es obligatoria")
        @Size(max = 100, message = "La especialidad no puede exceder 100 caracteres")
        String especialidad
) {

    public MaestroRequest {
        nombre = CamposDeTexto.limpiar(nombre);
        apellido = CamposDeTexto.limpiar(apellido);
        email = CamposDeTexto.normalizarEmail(email);
        especialidad = CamposDeTexto.limpiar(especialidad);
    }
}
