package com.escuela.controller;

import com.escuela.model.dto.AuthResponse;
import com.escuela.model.dto.LoginRequest;
import com.escuela.model.dto.RegisterRequest;
import com.escuela.model.entity.Rol;
import com.escuela.model.entity.Usuario;
import com.escuela.repository.UsuarioRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
    private static final String REGISTER_URL = "/api/auth/register";

    private static final String ADMIN_EMAIL = "admin@escuela.com";
    private static final String ADMIN_PASSWORD = "admin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        private RegisterRequest nuevoMaestro() {
            return new RegisterRequest("Sofía Vargas", "sofia.vargas@escuela.com",
                    "maestro123", Rol.MAESTRO);
        }

        @Test
        @DisplayName("con datos válidos devuelve 201 con el token del usuario creado")
        void registroExitoso() throws Exception {
            postJson(REGISTER_URL, nuevoMaestro())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.tipo").value("Bearer"))
                    .andExpect(jsonPath("$.email").value("sofia.vargas@escuela.com"))
                    .andExpect(jsonPath("$.nombre").value("Sofía Vargas"))
                    .andExpect(jsonPath("$.rol").value("MAESTRO"));
        }

        @Test
        @DisplayName("persiste el usuario con la contraseña cifrada")
        void passwordSeGuardaCifrada() throws Exception {
            postJson(REGISTER_URL, nuevoMaestro()).andExpect(status().isCreated());

            Optional<Usuario> guardado = usuarioRepository.findByEmail("sofia.vargas@escuela.com");
            assertThat(guardado).isPresent();
            assertThat(guardado.get().getRol()).isEqualTo(Rol.MAESTRO);
            assertThat(guardado.get().getPassword()).isNotEqualTo("maestro123");
            assertThat(passwordEncoder.matches("maestro123", guardado.get().getPassword())).isTrue();
        }

        @Test
        @DisplayName("el usuario recién registrado puede iniciar sesión de inmediato")
        void puedeIniciarSesionTrasRegistrarse() throws Exception {
            postJson(REGISTER_URL, nuevoMaestro()).andExpect(status().isCreated());

            AuthResponse respuesta = login("sofia.vargas@escuela.com", "maestro123");
            assertThat(respuesta.rol()).isEqualTo("MAESTRO");
        }

        @Test
        @DisplayName("con un email ya registrado devuelve 400 y no duplica el usuario")
        void emailDuplicado() throws Exception {
            long usuariosAntes = usuarioRepository.count();

            postJson(REGISTER_URL, new RegisterRequest("Otro Admin", ADMIN_EMAIL,
                    "admin123", Rol.ADMIN))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Ya existe un usuario con el email " + ADMIN_EMAIL));

            assertThat(usuarioRepository.count()).isEqualTo(usuariosAntes);
        }

        @Test
        @DisplayName("sin ningún campo devuelve 400 con el detalle de los cuatro obligatorios")
        void camposObligatorios() throws Exception {
            postJson(REGISTER_URL, new RegisterRequest(null, null, null, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados"))
                    .andExpect(jsonPath("$.detalles.nombre").value("El nombre es obligatorio"))
                    .andExpect(jsonPath("$.detalles.email").value("El email es obligatorio"))
                    .andExpect(jsonPath("$.detalles.password").value("La contraseña es obligatoria"))
                    .andExpect(jsonPath("$.detalles.rol")
                            .value("El rol es obligatorio (ADMIN, MAESTRO o ALUMNO)"));
        }

        @Test
        @DisplayName("con una contraseña más corta que el mínimo devuelve 400")
        void passwordDemasiadoCorta() throws Exception {
            postJson(REGISTER_URL, new RegisterRequest("Sofía Vargas",
                    "sofia.vargas@escuela.com", "12345", Rol.ALUMNO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.password")
                            .value("La contraseña debe tener entre 6 y 60 caracteres"));

            assertThat(usuarioRepository.findByEmail("sofia.vargas@escuela.com")).isEmpty();
        }

        @Test
        @DisplayName("con un nombre de más de 100 caracteres devuelve 400")
        void nombreDemasiadoLargo() throws Exception {
            postJson(REGISTER_URL, new RegisterRequest("N".repeat(101),
                    "sofia.vargas@escuela.com", "maestro123", Rol.ALUMNO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.nombre")
                            .value("El nombre no puede exceder 100 caracteres"));
        }

        @Test
        @DisplayName("con un rol inexistente devuelve 400 y no crea el usuario")
        void rolInexistente() throws Exception {
            postRaw(REGISTER_URL, """
                    {"nombre":"Sofía Vargas","email":"sofia.vargas@escuela.com",\
                    "password":"maestro123","rol":"SUPERADMIN"}""")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("El cuerpo de la petición es inválido o está mal formado"));

            assertThat(usuarioRepository.findByEmail("sofia.vargas@escuela.com")).isEmpty();
        }
    }
}
