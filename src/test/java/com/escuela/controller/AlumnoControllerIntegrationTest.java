package com.escuela.controller;

import com.escuela.model.dto.AlumnoRequest;
import com.escuela.model.entity.Alumno;
import com.escuela.repository.AlumnoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración de {@code /api/alumnos}, centrados en la seguridad por roles
 * declarada en {@code SecurityConfig}: la consulta la comparten ADMIN y MAESTRO,
 * mientras que crear, actualizar y borrar son exclusivos de ADMIN.
 *
 * <p>El rol se simula con {@code @WithMockUser} para cubrir la matriz completa.
 *
 * <p>Nota: los rechazos por rol los emite la cadena de filtros de seguridad, antes
 * del {@code DispatcherServlet}, así que no pasan por {@code GlobalExceptionHandler}
 * y la respuesta 403 no lleva cuerpo JSON propio. Por eso en esos casos se verifica
 * el estado y que el dato no haya cambiado en la base.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AlumnoController (integración)")
class AlumnoControllerIntegrationTest {

    private static final String ALUMNOS_URL = "/api/alumnos";
    private static final String ALUMNO_URL = "/api/alumnos/1";

    private static final String NUEVO_EMAIL = "sofia.vargas@alumnos.escuela.com";
    private static final String NUEVA_MATRICULA = "A2026099";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlumnoRepository alumnoRepository;

    private AlumnoRequest nuevoAlumno() {
        return new AlumnoRequest("Sofía", "Vargas", NUEVA_MATRICULA, NUEVO_EMAIL, "C");
    }

    /**
     * Construye la petición del método indicado, añadiendo un cuerpo válido cuando
     * el verbo lo necesita.
     */
    private MockHttpServletRequestBuilder peticion(String metodo, String path) throws Exception {
        MockHttpServletRequestBuilder builder =
                MockMvcRequestBuilders.request(HttpMethod.valueOf(metodo), path);

        if ("POST".equals(metodo) || "PUT".equals(metodo)) {
            builder.contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(nuevoAlumno()));
        }
        return builder;
    }

    @Nested
    @DisplayName("sin autenticación")
    class SinAutenticacion {

        @ParameterizedTest(name = "{0} {1} devuelve 401")
        @CsvSource({
                "GET,    /api/alumnos",
                "GET,    /api/alumnos/1",
                "POST,   /api/alumnos",
                "PUT,    /api/alumnos/1",
                "DELETE, /api/alumnos/1"
        })
        @DisplayName("ninguna operación es accesible sin token")
        void todoRequiereToken(String metodo, String path) throws Exception {
            mockMvc.perform(peticion(metodo, path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Se requiere un token JWT válido"));
        }

        @Test
        @DisplayName("un intento de creación sin token no toca la base de datos")
        void creacionSinTokenNoPersiste() throws Exception {
            mockMvc.perform(peticion("POST", ALUMNOS_URL))
                    .andExpect(status().isUnauthorized());

            assertThat(alumnoRepository.findByEmail(NUEVO_EMAIL)).isEmpty();
        }
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("como ADMIN")
    class ComoAdmin {

        @Test
        @DisplayName("lista los alumnos existentes")
        void listaAlumnos() throws Exception {
            long total = alumnoRepository.count();

            mockMvc.perform(get(ALUMNOS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value((int) total))
                    .andExpect(jsonPath("$[*].matricula")
                            .value(hasItems("A2026001", "A2026002", "A2026003")));
        }

        @Test
        @DisplayName("obtiene un alumno por id")
        void obtieneAlumnoPorId() throws Exception {
            mockMvc.perform(get(ALUMNO_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.matricula").value("A2026001"))
                    .andExpect(jsonPath("$.email").value("ana.lopez@escuela.com"));
        }

        @Test
        @DisplayName("con un id inexistente devuelve 404")
        void idInexistenteDevuelve404() throws Exception {
            mockMvc.perform(get(ALUMNOS_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Alumno con id 999 no encontrado"));
        }

        @Test
        @DisplayName("crea un alumno y lo persiste")
        void creaAlumno() throws Exception {
            mockMvc.perform(peticion("POST", ALUMNOS_URL))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.nombre").value("Sofía"))
                    .andExpect(jsonPath("$.matricula").value(NUEVA_MATRICULA));

            Optional<Alumno> guardado = alumnoRepository.findByEmail(NUEVO_EMAIL);
            assertThat(guardado).isPresent();
            assertThat(guardado.get().getGrupo()).isEqualTo("C");
        }

        @Test
        @DisplayName("con una matrícula ya usada devuelve 400")
        void matriculaDuplicada() throws Exception {
            AlumnoRequest repetido = new AlumnoRequest("Sofía", "Vargas", "A2026001",
                    NUEVO_EMAIL, "C");

            mockMvc.perform(post(ALUMNOS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(repetido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Ya existe un alumno con la matrícula A2026001"));

            assertThat(alumnoRepository.findByEmail(NUEVO_EMAIL)).isEmpty();
        }

        @Test
        @DisplayName("con un email ya usado devuelve 400")
        void emailDuplicado() throws Exception {
            AlumnoRequest repetido = new AlumnoRequest("Sofía", "Vargas", NUEVA_MATRICULA,
                    "ana.lopez@escuela.com", "C");

            mockMvc.perform(post(ALUMNOS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(repetido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Ya existe un alumno con el email ana.lopez@escuela.com"));
        }

        @Test
        @DisplayName("sin campos devuelve 400 con el detalle de cada uno")
        void validacionDeCampos() throws Exception {
            mockMvc.perform(post(ALUMNOS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AlumnoRequest(null, null, null, null, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.nombre").value("El nombre es obligatorio"))
                    .andExpect(jsonPath("$.detalles.apellido").value("El apellido es obligatorio"))
                    .andExpect(jsonPath("$.detalles.matricula").value("La matrícula es obligatoria"))
                    .andExpect(jsonPath("$.detalles.email").value("El email es obligatorio"))
                    .andExpect(jsonPath("$.detalles.grupo").value("El grupo es obligatorio"));
        }

        @Test
        @DisplayName("actualiza un alumno existente")
        void actualizaAlumno() throws Exception {
            AlumnoRequest cambios = new AlumnoRequest("Ana", "López", "A2026001",
                    "ana.lopez@escuela.com", "B");

            mockMvc.perform(MockMvcRequestBuilders.put(ALUMNO_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cambios)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.grupo").value("B"));

            assertThat(alumnoRepository.findById(1L)).get()
                    .extracting(Alumno::getGrupo).isEqualTo("B");
        }

        @Test
        @DisplayName("elimina un alumno existente")
        void eliminaAlumno() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete(ALUMNO_URL))
                    .andExpect(status().isNoContent());

            assertThat(alumnoRepository.findById(1L)).isEmpty();
        }

        @Test
        @DisplayName("al eliminar un id inexistente devuelve 404")
        void eliminarIdInexistente() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete(ALUMNOS_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Alumno con id 999 no encontrado"));
        }
    }

    @Nested
    @WithMockUser(roles = "MAESTRO")
    @DisplayName("como MAESTRO")
    class ComoMaestro {

        @Test
        @DisplayName("puede consultar el listado")
        void puedeListar() throws Exception {
            mockMvc.perform(get(ALUMNOS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("puede consultar un alumno por id")
        void puedeConsultarPorId() throws Exception {
            mockMvc.perform(get(ALUMNO_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matricula").value("A2026001"));
        }

        @Test
        @DisplayName("no puede crear alumnos: 403 y nada se persiste")
        void noPuedeCrear() throws Exception {
            mockMvc.perform(peticion("POST", ALUMNOS_URL))
                    .andExpect(status().isForbidden());

            assertThat(alumnoRepository.findByEmail(NUEVO_EMAIL)).isEmpty();
        }

        @Test
        @DisplayName("no puede actualizar alumnos: 403 y el registro no cambia")
        void noPuedeActualizar() throws Exception {
            mockMvc.perform(peticion("PUT", ALUMNO_URL))
                    .andExpect(status().isForbidden());

            assertThat(alumnoRepository.findById(1L)).get()
                    .extracting(Alumno::getMatricula).isEqualTo("A2026001");
        }

        @Test
        @DisplayName("no puede eliminar alumnos: 403 y el registro sigue ahí")
        void noPuedeEliminar() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete(ALUMNO_URL))
                    .andExpect(status().isForbidden());

            assertThat(alumnoRepository.findById(1L)).isPresent();
        }
    }

    @Nested
    @WithMockUser(roles = "ALUMNO")
    @DisplayName("como ALUMNO")
    class ComoAlumno {

        @Test
        @DisplayName("no puede consultar el listado de alumnos")
        void noPuedeListar() throws Exception {
            mockMvc.perform(get(ALUMNOS_URL))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("no puede consultar la ficha de otro alumno")
        void noPuedeConsultarPorId() throws Exception {
            mockMvc.perform(get(ALUMNO_URL))
                    .andExpect(status().isForbidden());
        }

        @ParameterizedTest(name = "{0} devuelve 403")
        @ValueSource(strings = {"POST", "PUT", "DELETE"})
        @DisplayName("tampoco puede modificar nada")
        void noPuedeModificar(String metodo) throws Exception {
            String path = "POST".equals(metodo) ? ALUMNOS_URL : ALUMNO_URL;

            mockMvc.perform(peticion(metodo, path))
                    .andExpect(status().isForbidden());

            assertThat(alumnoRepository.findById(1L)).isPresent();
            assertThat(alumnoRepository.findByEmail(NUEVO_EMAIL)).isEmpty();
        }
    }
}
