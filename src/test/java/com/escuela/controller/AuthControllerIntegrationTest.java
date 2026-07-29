package com.escuela.controller;

import com.escuela.model.dto.AuthResponse;
import com.escuela.model.dto.LoginRequest;
import com.escuela.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración de {@code /api/auth}: levantan el contexto completo (H2 con
 * los datos de {@code data.sql}) y atacan los endpoints a través de la cadena real
 * de filtros de seguridad.
 *
 * <p>La clase es {@code @Transactional}: los usuarios que se registran en los tests
 * se descartan al terminar cada uno, así que la semilla de {@code data.sql} se
 * mantiene intacta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController (integración)")
class AuthControllerIntegrationTest {

    private static final String LOGIN_URL = "/api/auth/login";

    private static final String ADMIN_EMAIL = "admin@escuela.com";
    private static final String ADMIN_PASSWORD = "admin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private ResultActions postJson(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions postRaw(String url, String json) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private AuthResponse login(String email, String password) throws Exception {
        String json = postJson(LOGIN_URL, new LoginRequest(email, password))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(json, AuthResponse.class);
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("con credenciales válidas devuelve 200 con el token y los datos del usuario")
        void loginExitoso() throws Exception {
            postJson(LOGIN_URL, new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.tipo").value("Bearer"))
                    .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                    .andExpect(jsonPath("$.nombre").value("Administrador"))
                    .andExpect(jsonPath("$.rol").value("ADMIN"));
        }

        @Test
        @DisplayName("no expone la contraseña en la respuesta")
        void respuestaSinPassword() throws Exception {
            postJson(LOGIN_URL, new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(resultado -> assertThat(
                            resultado.getResponse().getContentAsString())
                            .doesNotContain(ADMIN_PASSWORD));
        }

        @ParameterizedTest(name = "{2} inicia sesión y recibe su rol en el token")
        @CsvSource({
                "admin@escuela.com, admin123, ADMIN",
                "juan.perez@escuela.com, maestro123, MAESTRO",
                "ana.lopez@escuela.com, alumno123, ALUMNO"
        })
        @DisplayName("cada usuario semilla inicia sesión con su rol")
        void loginPorRol(String email, String password, String rol) throws Exception {
            AuthResponse respuesta = login(email, password);

            assertThat(respuesta.rol()).isEqualTo(rol);
            assertThat(respuesta.email()).isEqualTo(email);
            assertThat(jwtUtil.extraerRol(respuesta.token())).isEqualTo(rol);
        }

        @Test
        @DisplayName("el token emitido es válido y lleva el email y el rol como claims")
        void tokenEmitidoEsValido() throws Exception {
            AuthResponse respuesta = login(ADMIN_EMAIL, ADMIN_PASSWORD);

            assertThat(jwtUtil.esTokenValido(respuesta.token())).isTrue();
            assertThat(jwtUtil.extraerEmail(respuesta.token())).isEqualTo(ADMIN_EMAIL);
            assertThat(jwtUtil.extraerRol(respuesta.token())).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("con contraseña incorrecta devuelve 401 sin filtrar si el usuario existe")
        void passwordIncorrecta() throws Exception {
            postJson(LOGIN_URL, new LoginRequest(ADMIN_EMAIL, "password-equivocada"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                    .andExpect(jsonPath("$.path").value(LOGIN_URL))
                    .andExpect(jsonPath("$.token").doesNotExist());
        }

        @Test
        @DisplayName("con un email que no existe devuelve el mismo 401 que una contraseña mala")
        void usuarioInexistente() throws Exception {
            postJson(LOGIN_URL, new LoginRequest("fantasma@escuela.com", ADMIN_PASSWORD))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
        }

        @Test
        @DisplayName("sin email ni contraseña devuelve 400 con el detalle de cada campo")
        void camposObligatorios() throws Exception {
            postJson(LOGIN_URL, new LoginRequest(null, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados"))
                    .andExpect(jsonPath("$.detalles.email").value("El email es obligatorio"))
                    .andExpect(jsonPath("$.detalles.password").value("La contraseña es obligatoria"));
        }

        @Test
        @DisplayName("con un email mal formado devuelve 400")
        void emailInvalido() throws Exception {
            postJson(LOGIN_URL, new LoginRequest("no-es-un-email", ADMIN_PASSWORD))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.email")
                            .value("El email debe tener un formato válido"));
        }

        @Test
        @DisplayName("con un cuerpo JSON mal formado devuelve 400")
        void cuerpoMalFormado() throws Exception {
            postRaw(LOGIN_URL, "{\"email\": \"admin@escuela.com\", ")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("El cuerpo de la petición es inválido o está mal formado"));
        }
    }
}
