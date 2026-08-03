package com.escuela.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlumnoRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
        String apellido,

        @NotBlank(message = "La matrícula es obligatoria")
        @Size(max = 20, message = "La matrícula no puede exceder 20 caracteres")
        String matricula,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        @Size(max = 120, message = "El email no puede exceder 120 caracteres")
        String email,

        @NotBlank(message = "El grupo es obligatorio")
        @Size(max = 10, message = "El grupo no puede exceder 10 caracteres")
        String grupo
) {

    public AlumnoRequest {
        nombre = CamposDeTexto.limpiar(nombre);
        apellido = CamposDeTexto.limpiar(apellido);
        matricula = CamposDeTexto.limpiar(matricula);
        email = CamposDeTexto.normalizarEmail(email);
        grupo = CamposDeTexto.limpiar(grupo);
    }
}
