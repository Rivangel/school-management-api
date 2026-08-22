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

                        // Reportes
                        .requestMatchers("/api/reportes/**").hasAnyRole(ADMIN, MAESTRO, ALUMNO)

                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("""
                                    {"status":401,"error":"Unauthorized",\
                                    "message":"Se requiere un token JWT válido","path":"%s"}"""
                                    .formatted(request.getRequestURI()));
                        }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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
