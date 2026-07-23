package com.escuela.model.dto;

public record AuthResponse(
        String token,
        String tipo,
        String email,
        String nombre,
        String rol
) {

    public static AuthResponse bearer(String token, String email, String nombre, String rol) {
        return new AuthResponse(token, "Bearer", email, nombre, rol);
    }
}
