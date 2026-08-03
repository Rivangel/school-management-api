package com.escuela.model.dto;

import java.util.Locale;

/**
 * Normalización de los campos de texto que llegan en los DTOs de entrada.
 *
 * <p>Se aplica en el constructor compacto de cada record, es decir antes de que
 * corran las validaciones y antes de que el valor llegue al servicio. Así la
 * regla vale para todo el que construya el DTO y no hay que recordarla en cada
 * caso de uso.
 *
 * <p>El email se pasa a minúsculas por un motivo concreto: las comprobaciones de
 * unicidad (`existsByEmail`) comparan exacto, así que sin normalizar
 * `Ana@escuela.com` esquivaría el chequeo de un `ana@escuela.com` ya existente y
 * el choque acabaría saliendo como un 409 de la base en vez de un 400 con un
 * mensaje útil.
 */
final class CamposDeTexto {

    private CamposDeTexto() {
    }

    /** Quita los espacios de los extremos; deja pasar el nulo para que lo reporte {@code @NotBlank}. */
    static String limpiar(String valor) {
        return valor == null ? null : valor.strip();
    }

    /** Limpia y pasa a minúsculas, de modo que el email sea único sin importar cómo se escriba. */
    static String normalizarEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
