package com.escuela.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {

    public LoginRequest {
        // Mismo criterio que en el registro, para que el email entre igual en ambos
        email = CamposDeTexto.normalizarEmail(email);
    }
}
