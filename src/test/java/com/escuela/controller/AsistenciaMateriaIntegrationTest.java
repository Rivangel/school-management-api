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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración de {@code GET /api/asistencia/materia/{id}?fecha=}.
 *
 * <p>Se apoyan en {@code data.sql}: la materia 1 tiene asistencia de dos alumnos el
 * 2026-07-20 y de uno el 2026-07-21.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Asistencia por materia y fecha (integración)")
class AsistenciaMateriaIntegrationTest {

    private static final String URL = "/api/asistencia/materia/1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "MAESTRO")
    @DisplayName("devuelve lo registrado ese día")
    void devuelveLoDelDia() throws Exception {
        mockMvc.perform(get(URL).param("fecha", "2026-07-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fecha").value("2026-07-20"));
    }

    @Test
    @WithMockUser(roles = "MAESTRO")
    @DisplayName("otro día devuelve lo suyo, no todo el historial")
    void otroDiaDevuelveLoSuyo() throws Exception {
        mockMvc.perform(get(URL).param("fecha", "2026-07-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "MAESTRO")
    @DisplayName("un día sin lista devuelve vacío")
    void diaSinListaDevuelveVacio() throws Exception {
        mockMvc.perform(get(URL).param("fecha", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "MAESTRO")
    @DisplayName("sin la fecha responde 400, no 500")
    void sinFechaEs400() throws Exception {
        // El parámetro que falta lo lanzaba el catch-all como 500: no falló el servidor,
        // la petición venía incompleta.
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("fecha")));
    }

    @Test
    @WithMockUser(roles = "MAESTRO")
    @DisplayName("una fecha mal escrita también es 400")
    void fechaInvalidaEs400() throws Exception {
        mockMvc.perform(get(URL).param("fecha", "20 de julio"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("una materia que no existe es 404")
    void materiaInexistenteEs404() throws Exception {
        mockMvc.perform(get("/api/asistencia/materia/999").param("fecha", "2026-07-20"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "ana.lopez@escuela.com", roles = "ALUMNO")
    @DisplayName("el ALUMNO no puede ver la lista de su grupo")
    void alumnoNoVeElGrupo() throws Exception {
        // Sin la regla explícita habría caído en el `anyRequest().authenticated()`, que
        // le habría dejado pasar.
        mockMvc.perform(get(URL).param("fecha", "2026-07-20"))
                .andExpect(status().isForbidden());
    }
}
