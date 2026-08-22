package com.escuela.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración de {@code GET /api/maestros/me}.
 *
 * <p>El token dice el email y el rol, no el id del registro: sin este endpoint un
 * MAESTRO no puede pedir sus propias materias porque no sabe qué número es él.
 *
 * <p>Se apoyan en {@code data.sql}: Laura Gómez es el maestro 2 e imparte dos materias.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Identidad del usuario autenticado (integración)")
class IdentidadIntegrationTest {

    private static final String ME_URL = "/api/maestros/me";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "laura.gomez@escuela.com", roles = "MAESTRO")
    @DisplayName("devuelve el maestro del email autenticado")
    void devuelveElMaestroDelToken() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.apellido").value("Gómez"));
    }

    @Test
    @WithMockUser(username = "laura.gomez@escuela.com", roles = "MAESTRO")
    @DisplayName("/me no se confunde con /{id}")
    void meNoSeInterpretaComoId() throws Exception {
        // Si el router probara `/{id}` primero, "me" no sería un número y la respuesta
        // sería el 400 del `MethodArgumentTypeMismatchException`.
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@escuela.com", roles = "ADMIN")
    @DisplayName("un ADMIN no tiene maestro vinculado: 404, no 500")
    void adminSinMaestroVinculado() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("admin@escuela.com")));
    }

    @Test
    @WithMockUser(username = "ana.lopez@escuela.com", roles = "ALUMNO")
    @DisplayName("un ALUMNO no puede leer la sección de maestros")
    void alumnoNoPuedeLeerMaestros() throws Exception {
        // Lo decide `SecurityConfig`, que reserva el GET de maestros a ADMIN y MAESTRO:
        // este endpoint no abre una puerta nueva.
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isForbidden());
    }
}
