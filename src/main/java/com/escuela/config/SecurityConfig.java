package com.escuela.config;

import com.escuela.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String MAESTRO = "MAESTRO";
    private static final String ALUMNO = "ALUMNO";

    private final JwtFilter jwtFilter;

    /** Orígenes del frontend autorizados; en dev es el `ng serve` de Angular. */
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Público: autenticación y documentación
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Alumnos, maestros y materias: gestión exclusiva del ADMIN,
                        // consulta también para MAESTRO
                        // Antes de la regla general: `/me` devuelve el registro del propio
                        // usuario, así que el ALUMNO sí puede pedirlo — es lo que le
                        // permite consultar sus notas, que se piden por id. Sigue sin poder
                        // leer a los demás.
                        .requestMatchers(HttpMethod.GET, "/api/alumnos/me").hasAnyRole(ADMIN, MAESTRO, ALUMNO)
                        .requestMatchers(HttpMethod.GET, "/api/alumnos/**").hasAnyRole(ADMIN, MAESTRO)
                        .requestMatchers("/api/alumnos/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/maestros/**").hasAnyRole(ADMIN, MAESTRO)
                        .requestMatchers("/api/maestros/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/materias/**").hasAnyRole(ADMIN, MAESTRO, ALUMNO)
                        .requestMatchers("/api/materias/**").hasRole(ADMIN)

                        // Calificaciones y asistencia: los maestros registran (solo de sus
                        // materias, validado en el servicio); los alumnos solo consultan lo suyo
                        .requestMatchers(HttpMethod.POST, "/api/calificaciones").hasAnyRole(ADMIN, MAESTRO)
                        .requestMatchers(HttpMethod.GET, "/api/calificaciones/alumno/**").hasAnyRole(ADMIN, MAESTRO, ALUMNO)
                        .requestMatchers(HttpMethod.GET, "/api/calificaciones/materia/**").hasAnyRole(ADMIN, MAESTRO)
                        .requestMatchers(HttpMethod.POST, "/api/asistencia").hasAnyRole(ADMIN, MAESTRO)
                        .requestMatchers(HttpMethod.GET, "/api/asistencia/alumno/**").hasAnyRole(ADMIN, MAESTRO, ALUMNO)
                        // La lista de un grupo entero no es del ALUMNO, como las notas por
                        // materia. Va explícita porque si no cae en el `anyRequest()` de
                        // abajo, que sólo pide estar autenticado: un endpoint nuevo bajo un
                        // prefijo ya usado no hereda las reglas de sus vecinos.
                        .requestMatchers(HttpMethod.GET, "/api/asistencia/materia/**").hasAnyRole(ADMIN, MAESTRO)

                        // Reportes
                        .requestMatchers("/api/reportes/**").hasAnyRole(ADMIN, MAESTRO, ALUMNO)

                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        // Quien no se ha identificado: 401. El cuerpo se escribe aquí mismo
                        // en vez de con `sendError`, para no reenviar la petición a `/error`.
                        .authenticationEntryPoint((request, response, ex) ->
                                escribirError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Unauthorized", "Se requiere un token JWT válido",
                                        request.getRequestURI()))
                        // Quien sí se identificó pero no tiene el rol: **403, no 401**, y la
                        // diferencia no es cosmética. El frontend lee un 401 como "la sesión
                        // caducó", cierra la sesión y manda al login (`error-interceptor.ts`),
                        // así que responder 401 aquí echa de la aplicación a un usuario cuya
                        // sesión está perfectamente viva por pedir algo que no le tocaba.
                        //
                        // Sin este handler actúa el de por defecto, que llama a `sendError`:
                        // el contenedor reenvía a `/error` —que no es público— y esa segunda
                        // vuelta por la cadena acaba en el `authenticationEntryPoint` de
                        // arriba, de modo que el 403 sale convertido en 401. **MockMvc no
                        // ejecuta ese reenvío**, así que los tests de rol veían el 403 correcto
                        // mientras el servidor real respondía 401; por eso el test que fija
                        // esto (`AccesoDenegadoIntegrationTest`) levanta un servidor de verdad.
                        .accessDeniedHandler((request, response, ex) ->
                                escribirError(response, HttpServletResponse.SC_FORBIDDEN,
                                        "Forbidden",
                                        "No tienes permisos para acceder a este recurso",
                                        request.getRequestURI())))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Escribe un error de seguridad como JSON, con la misma forma que los del
     * {@code GlobalExceptionHandler} para que el cliente no tenga que distinguir
     * de dónde salió el fallo.
     *
     * <p>Escribe directamente en la respuesta en lugar de usar {@code sendError}
     * a propósito: {@code sendError} desencadena el reenvío a {@code /error}, que
     * vuelve a pasar por la cadena de seguridad y cambia el código de estado.
     */
    private static void escribirError(HttpServletResponse response, int estado, String error,
                                      String mensaje, String ruta) throws IOException {
        response.setStatus(estado);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}"
                        .formatted(estado, error, mensaje, ruta));
    }

    /**
     * CORS para el frontend. Los orígenes se declaran uno a uno (nunca `*`) porque
     * el navegador manda el JWT en la cabecera `Authorization`, y se exponen sólo
     * los verbos y cabeceras que la API usa de verdad.
     *
     * <p>`allowCredentials` queda en falso a propósito: la sesión viaja en el token,
     * no en cookies, así que no hace falta relajar la política del navegador.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // El front necesita leer estas dos para descargar la boleta PDF
        config.setExposedHeaders(List.of("Content-Disposition", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
