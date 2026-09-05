package com.escuela.config;

import com.escuela.model.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Códigos de las denegaciones de la cadena de seguridad, con un servidor de verdad.
 *
 * <p><b>Por qué no MockMvc.</b> El resto de los tests de rol usan MockMvc y son
 * correctos, pero no habrían visto este fallo: cuando la cadena deniega el acceso,
 * el handler por defecto llama a {@code sendError}, y quien convierte eso en un
 * reenvío a {@code /error} es el <b>contenedor de servlets</b> — que MockMvc no
 * ejecuta. En el servidor real esa segunda vuelta por la cadena acababa en el
 * {@code authenticationEntryPoint} y el 403 salía como <b>401</b>, de modo que los
 * tests pasaban en verde mientras la aplicación desplegada echaba al usuario.
 *
 * <p><b>Por qué importa la diferencia.</b> El frontend trata un 401 como sesión
 * caducada: cierra la sesión y manda al login. Un MAESTRO que pulsara "eliminar"
 * acababa en la pantalla de acceso con un "tu sesión caducó" que no era verdad.
 *
 * <p>Por eso este test levanta el puerto: es la única forma de que el reenvío de
 * error ocurra y el código que ve el navegador sea el que se comprueba aquí.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Denegaciones de la cadena de seguridad (servidor real)")
class AccesoDenegadoIntegrationTest {

    @LocalServerPort
    private int puerto;

    @Autowired
    private TestRestTemplate rest;

    @Nested
    @DisplayName("un usuario autenticado sin el rol necesario")
    class SinElRol {

        @Test
        @DisplayName("recibe 403 al leer un listado que no le toca, no 401")
        void lecturaDenegadaEs403() {
            ResponseEntity<String> respuesta = comoAlumno(HttpMethod.GET, "/api/alumnos?size=1");

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(respuesta.getBody())
                    .contains("\"status\":403")
                    .contains("No tienes permisos");
        }

        @Test
        @DisplayName("recibe 403 al ver las notas de una materia entera")
        void notasDeMateriaDenegadasEs403() {
            assertThat(comoAlumno(HttpMethod.GET, "/api/calificaciones/materia/1").getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("recibe 403 al intentar una escritura reservada al ADMIN")
        void escrituraDenegadaEs403() {
            ResponseEntity<String> respuesta =
                    comoMaestro(HttpMethod.DELETE, "/api/alumnos/1");

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("el cuerpo del 403 dice la ruta pedida, no /error")
        void elCuerpoNoDelataElReenvio() {
            assertThat(comoAlumno(HttpMethod.GET, "/api/maestros?size=1").getBody())
                    .contains("\"path\":\"/api/maestros\"")
                    .doesNotContain("/error");
        }
    }

    @Nested
    @DisplayName("sin credenciales")
    class SinCredenciales {

        @Test
        @DisplayName("sigue siendo 401: ahí sí falta identificarse")
        void sinTokenEs401() {
            ResponseEntity<String> respuesta =
                    rest.getForEntity(url("/api/alumnos"), String.class);

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(respuesta.getBody()).contains("\"status\":401");
        }

        @Test
        @DisplayName("un token inservible es 401, no 403")
        void tokenInservibleEs401() {
            assertThat(conToken("no.es.un.jwt", HttpMethod.GET, "/api/alumnos").getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    private ResponseEntity<String> comoAlumno(HttpMethod metodo, String ruta) {
        return conToken(token("ana.lopez@escuela.com", "alumno123"), metodo, ruta);
    }

    private ResponseEntity<String> comoMaestro(HttpMethod metodo, String ruta) {
        return conToken(token("juan.perez@escuela.com", "maestro123"), metodo, ruta);
    }

    private ResponseEntity<String> conToken(String token, HttpMethod metodo, String ruta) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setBearerAuth(token);
        return rest.exchange(url(ruta), metodo, new HttpEntity<>(cabeceras), String.class);
    }

    /** Un token de verdad: el filtro tiene que autenticar para que la denegación sea por rol. */
    @SuppressWarnings("unchecked")
    private String token(String email, String password) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        LoginRequest credenciales = new LoginRequest(email, password);

        Map<String, Object> cuerpo = rest.postForObject(
                url("/api/auth/login"), new HttpEntity<>(credenciales, cabeceras), Map.class);

        assertThat(cuerpo).as("el login de %s tenía que funcionar", email).isNotNull();
        return (String) cuerpo.get("token");
    }

    private String url(String ruta) {
        return "http://localhost:" + puerto + ruta;
    }
}
