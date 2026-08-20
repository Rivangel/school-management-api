package com.escuela.controller;

import com.escuela.model.dto.AlumnoRequest;
import com.escuela.model.dto.CalificacionRequest;
import com.escuela.model.dto.MaestroRequest;
import com.escuela.repository.AlumnoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la validación de entrada: comprueban que los datos malos se rechazan
 * con un 400 y un mensaje que dice qué campo falló, en vez de llegar a la base
 * y salir como un 409 o un 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
@DisplayName("Validación de entrada (integración)")
class ValidacionIntegrationTest {

    private static final String ALUMNOS_URL = "/api/alumnos";
    private static final String MAESTROS_URL = "/api/maestros";
    private static final String CALIFICACIONES_URL = "/api/calificaciones";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlumnoRepository alumnoRepository;

    private String json(Object cuerpo) throws Exception {
        return objectMapper.writeValueAsString(cuerpo);
    }

    @Nested
    @DisplayName("email")
    class ValidacionDeEmail {

        @Test
        @DisplayName("uno más largo que la columna se rechaza con 400, no revienta en la base")
        void emailDemasiadoLargo() throws Exception {
            // 125 caracteres y con formato válido (la parte local no pasa de 64, que es
            // el límite del RFC), para que salte @Size y no @Email. La columna admite 120.
            String emailLargo = "a".repeat(60) + "@" + "b".repeat(60) + ".com";
            AlumnoRequest request = new AlumnoRequest(
                    "Pedro", "Sánchez", "A2026050", emailLargo, "B");

            mockMvc.perform(post(ALUMNOS_URL).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.email")
                            .value("El email no puede exceder 120 caracteres"));

            assertThat(alumnoRepository.existsByMatricula("A2026050")).isFalse();
        }

        @Test
        @DisplayName("un duplicado que sólo cambia en mayúsculas se rechaza con 400, no con 409 de la base")
        void duplicadoPorCapitalizacion() throws Exception {
            // ana.lopez@escuela.com ya existe en data.sql
            AlumnoRequest request = new AlumnoRequest(
                    "Otra", "Persona", "A2026051", "ANA.LOPEZ@ESCUELA.COM", "B");

            mockMvc.perform(post(ALUMNOS_URL).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Ya existe un alumno con el email ana.lopez@escuela.com"));

            assertThat(alumnoRepository.existsByMatricula("A2026051")).isFalse();
        }

        @Test
        @DisplayName("se guarda normalizado a minúsculas y sin espacios sobrantes")
        void seGuardaNormalizado() throws Exception {
            AlumnoRequest request = new AlumnoRequest(
                    "  Pedro  ", "  Sánchez ", " A2026052 ", "  Pedro.Sanchez@Escuela.com  ", " B ");

            mockMvc.perform(post(ALUMNOS_URL).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("pedro.sanchez@escuela.com"))
                    .andExpect(jsonPath("$.nombre").value("Pedro"))
                    .andExpect(jsonPath("$.matricula").value("A2026052"));

            assertThat(alumnoRepository.findByEmail("pedro.sanchez@escuela.com")).isPresent();
        }

        @ParameterizedTest(name = "\"{0}\" no es un email válido")
        @ValueSource(strings = {"sin-arroba", "@sin-usuario.com", "espacio en@medio.com"})
        @DisplayName("un formato inválido se rechaza")
        void formatoInvalido(String email) throws Exception {
            MaestroRequest request = new MaestroRequest("Sofía", "Martínez", email, "Física");

            mockMvc.perform(post(MAESTROS_URL).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.email").exists());
        }
    }

    @Nested
    @DisplayName("calificaciones")
    class ValidacionDeCalificaciones {

        @ParameterizedTest(name = "el periodo \"{0}\" se rechaza")
        @ValueSource(strings = {"2026", "26-1", "2026-3", "primer semestre"})
        @DisplayName("el periodo debe seguir el formato AAAA-S")
        void periodoConFormatoInvalido(String periodo) throws Exception {
            CalificacionRequest request = new CalificacionRequest(
                    1L, 1L, new BigDecimal("9.0"), periodo);

            mockMvc.perform(post(CALIFICACIONES_URL).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.periodo").exists());
        }

        @Test
        @DisplayName("un periodo con el formato correcto se acepta")
        void periodoValido() throws Exception {
            CalificacionRequest request = new CalificacionRequest(
                    1L, 1L, new BigDecimal("9.0"), "2026-2");

            mockMvc.perform(post(CALIFICACIONES_URL).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isCreated());
        }

        @ParameterizedTest(name = "la calificación {0} queda fuera del rango 0-10")
        @ValueSource(strings = {"-0.5", "10.5", "100"})
        @DisplayName("la calificación se mantiene entre 0 y 10")
        void calificacionFueraDeRango(String valor) throws Exception {
            CalificacionRequest request = new CalificacionRequest(
                    1L, 1L, new BigDecimal(valor), "2026-1");

            mockMvc.perform(post(CALIFICACIONES_URL).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.calificacion").exists());
        }

        @Test
        @DisplayName("un id negativo se rechaza por validación, no como 404")
        void idNegativo() throws Exception {
            CalificacionRequest request = new CalificacionRequest(
                    -1L, 1L, new BigDecimal("9.0"), "2026-1");

            mockMvc.perform(post(CALIFICACIONES_URL).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.alumnoId")
                            .value("El id del alumno debe ser un número positivo"));
        }
    }

    @Test
    @DisplayName("un id que no es un número es un 400, no un 500")
    void idNoNumericoEnLaRuta() throws Exception {
        // `/api/alumnos/abc` es una dirección que cualquiera puede teclear. Sin un handler
        // propio la recoge el catch-all y sale como un 500, que además de mentir sugiere
        // que el fallo está en el servidor.
        mockMvc.perform(get(ALUMNOS_URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("El valor 'abc' no es válido para el parámetro 'id'"));
    }

    @Test
    @DisplayName("un campo con sólo espacios cuenta como vacío")
    void camposEnBlanco() throws Exception {
        AlumnoRequest request = new AlumnoRequest("   ", "Sánchez", "A2026053", "p.s@escuela.com", "B");

        mockMvc.perform(post(ALUMNOS_URL).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.nombre").value("El nombre es obligatorio"));
    }
}
