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

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración del filtro {@code ?maestroId=} de {@code GET /api/materias}.
 *
 * <p>Se apoyan en los datos de {@code data.sql}: el maestro 1 (Juan Pérez) imparte una
 * materia y el 2 (Laura Gómez), dos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
@DisplayName("Filtro de materias por maestro (integración)")
class MateriaFiltroIntegrationTest {

    private static final String MATERIAS_URL = "/api/materias";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("devuelve sólo las materias del maestro pedido")
    void devuelveSoloLasDelMaestro() throws Exception {
        mockMvc.perform(get(MATERIAS_URL).param("maestroId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nombre")
                        .value(contains("Bases de Datos", "Programación Orientada a Objetos")))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("sin el filtro siguen saliendo todas")
    void sinFiltroSalenTodas() throws Exception {
        mockMvc.perform(get(MATERIAS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("los totales cuentan las materias del maestro, no todas")
    void losTotalesSonLosDelFiltro() throws Exception {
        // Es lo que evita que el paginador del frontend calcule páginas que no existen:
        // filtrar y paginar tienen que contar lo mismo.
        mockMvc.perform(get(MATERIAS_URL)
                        .param("maestroId", "2")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("el filtro se combina con el orden pedido")
    void seCombinaConElOrden() throws Exception {
        mockMvc.perform(get(MATERIAS_URL)
                        .param("maestroId", "2")
                        .param("sort", "creditos,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nombre")
                        .value(contains("Programación Orientada a Objetos", "Bases de Datos")));
    }

    @Test
    @DisplayName("un maestro que no existe devuelve una página vacía, no un 404")
    void maestroInexistenteDevuelvePaginaVacia() throws Exception {
        // Es un filtro, no un recurso: no encontrar nada es una respuesta válida.
        mockMvc.perform(get(MATERIAS_URL).param("maestroId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("un maestroId que no es un número es un 400, no un 500")
    void maestroIdNoNumericoDevuelve400() throws Exception {
        // La barra de direcciones es texto que cualquiera edita: un parámetro mal escrito
        // no es un fallo del servidor.
        mockMvc.perform(get(MATERIAS_URL).param("maestroId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("El valor 'abc' no es válido para el parámetro 'maestroId'"));
    }
}
