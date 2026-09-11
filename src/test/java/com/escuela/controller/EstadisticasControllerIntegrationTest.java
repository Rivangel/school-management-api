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
 * Tests de integración de {@code GET /api/estadisticas}.
 *
 * <p>Se apoyan en {@code data.sql}: 3 alumnos, 2 maestros, 3 materias, 7
 * calificaciones y 7 registros de asistencia. Los totales esperados están
 * calculados a mano en el propio test, no repetidos de la implementación.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Estadísticas del dashboard (integración)")
class EstadisticasControllerIntegrationTest {

    private static final String URL = "/api/estadisticas";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("el ADMIN recibe los conteos y promedios de todo el centro")
    void adminRecibeLosConteos() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlumnos").value(3))
                .andExpect(jsonPath("$.totalMaestros").value(2))
                .andExpect(jsonPath("$.totalMaterias").value(3))
                // (9.50+8.70+10.00+7.20+5.80+8.00+9.10) / 7 = 8.328571... -> 8.33
                .andExpect(jsonPath("$.promedioGeneral").value(8.33))
                .andExpect(jsonPath("$.promedioPorMateria.length()").value(3))
                .andExpect(jsonPath("$.promedioPorMateria[0].materiaNombre").value("Bases de Datos"))
                .andExpect(jsonPath("$.promedioPorMateria[0].promedio").value(9.55))
                .andExpect(jsonPath("$.promedioPorMateria[0].cantidadCalificaciones").value(2))
                .andExpect(jsonPath("$.promedioPorMateria[1].materiaNombre").value("Matemáticas I"))
                .andExpect(jsonPath("$.promedioPorMateria[1].promedio").value(8.35))
                .andExpect(jsonPath("$.promedioPorMateria[2].materiaNombre")
                        .value("Programación Orientada a Objetos"))
                .andExpect(jsonPath("$.promedioPorMateria[2].promedio").value(7.50));
    }

    @Test
    @WithMockUser(roles = "MAESTRO")
    @DisplayName("el MAESTRO recibe la asistencia general y por materia")
    void maestroRecibeLaAsistencia() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                // 5 presentes de 7 registros = 71.428... -> 71.43
                .andExpect(jsonPath("$.asistenciaGeneral.presentes").value(5))
                .andExpect(jsonPath("$.asistenciaGeneral.ausentes").value(2))
                .andExpect(jsonPath("$.asistenciaGeneral.total").value(7))
                .andExpect(jsonPath("$.asistenciaGeneral.porcentaje").value(71.43))
                .andExpect(jsonPath("$.asistenciaPorMateria.length()").value(3))
                .andExpect(jsonPath("$.asistenciaPorMateria[0].materiaNombre").value("Bases de Datos"))
                .andExpect(jsonPath("$.asistenciaPorMateria[0].porcentaje").value(0.0))
                .andExpect(jsonPath("$.asistenciaPorMateria[1].materiaNombre").value("Matemáticas I"))
                .andExpect(jsonPath("$.asistenciaPorMateria[1].porcentaje").value(100.0))
                .andExpect(jsonPath("$.asistenciaPorMateria[2].materiaNombre")
                        .value("Programación Orientada a Objetos"))
                .andExpect(jsonPath("$.asistenciaPorMateria[2].porcentaje").value(66.67));
    }

    @Test
    @WithMockUser(username = "ana.lopez@escuela.com", roles = "ALUMNO")
    @DisplayName("el ALUMNO no puede consultar las estadísticas del centro")
    void alumnoNoPuedeConsultar() throws Exception {
        // Sin la regla explícita en SecurityConfig habría caído en el
        // `anyRequest().authenticated()`, que le habría dejado pasar.
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden());
    }
}
