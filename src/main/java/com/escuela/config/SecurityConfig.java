package com.escuela.config;

import com.escuela.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String MAESTRO = "MAESTRO";
    private static final String ALUMNO = "ALUMNO";

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Público: autenticación y documentación
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Alumnos, maestros y materias: gestión exclusiva del ADMIN,
                        // consulta también para MAESTRO
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
