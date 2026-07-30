package com.escuela.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración de la paginación y el ordenamiento que comparten los tres
 * listados (`/api/alumnos`, `/api/maestros`, `/api/materias`).
 *
 * <p>Se apoyan en los datos de {@code data.sql}: 3 alumnos, 2 maestros y 3 materias.
 * La clase es {@code @Transactional} para no dejar rastro entre tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
@DisplayName("Paginación y ordenamiento (integración)")
class PaginacionIntegrationTest {

    private static final String ALUMNOS_URL = "/api/alumnos";
    private static final String MAESTROS_URL = "/api/maestros";
    private static final String MATERIAS_URL = "/api/materias";

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("sin parámetros")
    class ValoresPorDefecto {

        @ParameterizedTest(name = "{0} devuelve la primera página de 20")
        @ValueSource(strings = {ALUMNOS_URL, MAESTROS_URL, MATERIAS_URL})
        @DisplayName("todos los listados aplican los mismos valores por defecto")
        void primeraPaginaDeVeinte(String url) throws Exception {
            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("los alumnos salen ordenados por apellido")
        void alumnosOrdenadosPorApellido() throws Exception {
            mockMvc.perform(get(ALUMNOS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].apellido")
                            .value(contains("López", "Ramírez", "Torres")));
        }

        @Test
        @DisplayName("las materias salen ordenadas por nombre")
        void materiasOrdenadasPorNombre() throws Exception {
            mockMvc.perform(get(MATERIAS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].nombre").value(contains(
                            "Bases de Datos", "Matemáticas I", "Programación Orientada a Objetos")));
        }
    }

    @Nested
    @DisplayName("con page y size")
    class Paginado {

        @Test
        @DisplayName("la primera página trae solo los elementos que caben")
        void primeraPagina() throws Exception {
            mockMvc.perform(get(ALUMNOS_URL).param("page", "0").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[*].matricula")
                            .value(contains("A2026001", "A2026002")))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(false));
        }

        @Test
        @DisplayName("la última página trae el resto sin repetir elementos")
        void ultimaPagina() throws Exception {
            mockMvc.perform(get(ALUMNOS_URL).param("page", "1").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[*].matricula").value(contains("A2026003")))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.first").value(false))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("una página más allá de la última viene vacía pero con los totales correctos")
        void paginaFueraDeRango() throws Exception {
            mockMvc.perform(get(ALUMNOS_URL).param("page", "9").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("un size desmedido se recorta al máximo configurado")
        void sizeSeRecortaAlMaximo() throws Exception {
            mockMvc.perform(get(ALUMNOS_URL).param("size", "5000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(100));
        }
    }

    @Nested
    @DisplayName("con sort")
    class Ordenamiento {

        @Test
        @DisplayName("respeta la dirección descendente")
        void ordenDescendente() throws Exception {
            mockMvc.perform(get(ALUMNOS_URL).param("sort", "matricula,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].matricula")
                            .value(contains("A2026003", "A2026002", "A2026001")));
        }

        @Test
        @DisplayName("ordena por una propiedad distinta a la de por defecto")
        void ordenPorOtraPropiedad() throws Exception {
            mockMvc.perform(get(MAESTROS_URL).param("sort", "especialidad,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].especialidad")
                            .value(contains("Ciencias de la Computación", "Matemáticas")));
        }

        @Test
        @DisplayName("admite propiedades anidadas de la relación con maestro")
        void ordenPorPropiedadAnidada() throws Exception {
            mockMvc.perform(get(MATERIAS_URL)
                            .param("sort", "maestro.apellido,asc")
                            .param("sort", "nombre,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].nombre").value(contains(
                            "Bases de Datos", "Programación Orientada a Objetos", "Matemáticas I")));
        }

        @Test
        @DisplayName("el orden se mantiene al cambiar de página")
        void ordenSeMantieneEntrePaginas() throws Exception {
            mockMvc.perform(get(ALUMNOS_URL)
                            .param("sort", "matricula,desc")
                            .param("page", "1")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].matricula").value(contains("A2026001")));
        }

        @ParameterizedTest(name = "{0} rechaza un sort inexistente con 400")
        @ValueSource(strings = {ALUMNOS_URL, MAESTROS_URL, MATERIAS_URL})
        @DisplayName("una propiedad de ordenamiento inexistente es un 400, no un 500")
        void sortInexistenteDevuelve400(String url) throws Exception {
            mockMvc.perform(get(url).param("sort", "columnaFantasma,asc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message")
                            .value("No se puede ordenar por 'columnaFantasma': la propiedad no existe"));
        }
    }
}
