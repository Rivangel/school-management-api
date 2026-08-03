package com.escuela.config;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la política CORS que consume el frontend Angular.
 *
 * <p>Lo importante que fijan: el preflight se resuelve <em>antes</em> de la
 * autenticación (si no, el navegador nunca llegaría a mandar el token) y los
 * orígenes ajenos no reciben la cabecera que los autorizaría.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CORS (integración)")
class CorsIntegrationTest {

    private static final String FRONTEND = "http://localhost:4200";
    private static final String ORIGEN_AJENO = "http://sitio-malicioso.com";

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("preflight desde el frontend autorizado")
    class PreflightAutorizado {

        @ParameterizedTest(name = "permite {0}")
        @ValueSource(strings = {"GET", "POST", "PUT", "DELETE"})
        @DisplayName("los verbos que usa la API vienen autorizados")
        void permiteLosVerbosDeLaApi(String metodo) throws Exception {
            mockMvc.perform(options("/api/alumnos")
                            .header("Origin", FRONTEND)
                            .header("Access-Control-Request-Method", metodo))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND));
        }

        @Test
        @DisplayName("el preflight no requiere token: se resuelve antes de la autenticación")
        void preflightNoRequiereToken() throws Exception {
            mockMvc.perform(options("/api/alumnos")
                            .header("Origin", FRONTEND)
                            .header("Access-Control-Request-Method", "GET")
                            .header("Access-Control-Request-Headers", "Authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Headers", "Authorization"));
        }

        @Test
        @DisplayName("expone Content-Disposition para que el front descargue la boleta PDF")
        void exponeContentDisposition() throws Exception {
            mockMvc.perform(options("/api/reportes/boleta/1")
                            .header("Origin", FRONTEND)
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Expose-Headers",
                            "Content-Disposition, Content-Type"));
        }
    }

    @Nested
    @DisplayName("desde un origen no autorizado")
    class OrigenNoAutorizado {

        @Test
        @DisplayName("el preflight se rechaza")
        void preflightRechazado() throws Exception {
            mockMvc.perform(options("/api/alumnos")
                            .header("Origin", ORIGEN_AJENO)
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @Transactional
        @WithMockUser(roles = "ADMIN")
        @DisplayName("una petición real no recibe la cabecera que la autorizaría")
        void peticionRealSinCabeceraDePermiso() throws Exception {
            mockMvc.perform(get("/api/alumnos").header("Origin", ORIGEN_AJENO))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("una petición real desde el frontend sí lleva la cabecera de permiso")
    void peticionRealDesdeElFrontend() throws Exception {
        mockMvc.perform(get("/api/alumnos").header("Origin", FRONTEND))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND));
    }
}
