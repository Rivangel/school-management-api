package com.escuela.security;

import com.escuela.exception.OperacionNoPermitidaException;
import com.escuela.model.entity.Alumno;
import com.escuela.model.entity.Maestro;
import com.escuela.model.entity.Materia;
import com.escuela.model.entity.Rol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CurrentUserService")
class CurrentUserServiceTest {

    private CurrentUserService currentUserService;
    private Alumno alumno;
    private Materia materia;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService();
        alumno = new Alumno("Ana", "Torres", "A-2026-001", "ana.torres@escuela.com", "3A");
        alumno.setId(1L);
        Maestro maestro = new Maestro("Laura", "Mendoza", "laura.mendoza@escuela.com", "Matemáticas");
        maestro.setId(5L);
        materia = new Materia("Álgebra I", 8, maestro);
        materia.setId(10L);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String email, Rol rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))));
    }

    @Nested
    @DisplayName("sin sesión")
    class SinSesion {

        @Test
        @DisplayName("getEmail devuelve null y tieneRol devuelve false")
        void sinAutenticacion() {
            assertThat(currentUserService.getEmail()).isNull();
            assertThat(currentUserService.tieneRol(Rol.ADMIN)).isFalse();
        }
    }

    @Nested
    @DisplayName("tieneRol")
    class TieneRol {

        @Test
        @DisplayName("reconoce el rol del usuario autenticado")
        void reconoceElRol() {
            autenticar("laura.mendoza@escuela.com", Rol.MAESTRO);

            assertThat(currentUserService.tieneRol(Rol.MAESTRO)).isTrue();
            assertThat(currentUserService.tieneRol(Rol.ADMIN)).isFalse();
            assertThat(currentUserService.tieneRol(Rol.ALUMNO)).isFalse();
            assertThat(currentUserService.getEmail()).isEqualTo("laura.mendoza@escuela.com");
        }
    }

    @Nested
    @DisplayName("validarAccesoAlumno")
    class ValidarAccesoAlumno {

        @Test
        @DisplayName("permite que un alumno consulte su propia información")
        void alumnoAccedeASusDatos() {
            autenticar("ana.torres@escuela.com", Rol.ALUMNO);

            assertThatCode(() -> currentUserService.validarAccesoAlumno(alumno))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("compara el email sin distinguir mayúsculas")
        void comparaEmailSinDistinguirMayusculas() {
            autenticar("ANA.TORRES@Escuela.com", Rol.ALUMNO);

            assertThatCode(() -> currentUserService.validarAccesoAlumno(alumno))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rechaza que un alumno consulte información de otro")
        void alumnoNoAccedeADatosAjenos() {
            autenticar("otro.alumno@escuela.com", Rol.ALUMNO);

            assertThatThrownBy(() -> currentUserService.validarAccesoAlumno(alumno))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("su propia información");
        }

        @Test
        @DisplayName("no restringe a ADMIN ni a MAESTRO")
        void adminYMaestroNoTienenRestriccion() {
            autenticar("admin@escuela.com", Rol.ADMIN);
            assertThatCode(() -> currentUserService.validarAccesoAlumno(alumno))
                    .doesNotThrowAnyException();

            autenticar("laura.mendoza@escuela.com", Rol.MAESTRO);
            assertThatCode(() -> currentUserService.validarAccesoAlumno(alumno))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validarMaestroDeMateria")
    class ValidarMaestroDeMateria {

        @Test
        @DisplayName("permite que un maestro registre en su propia materia")
        void maestroRegistraEnSuMateria() {
            autenticar("laura.mendoza@escuela.com", Rol.MAESTRO);

            assertThatCode(() -> currentUserService.validarMaestroDeMateria(materia))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("compara el email sin distinguir mayúsculas")
        void comparaEmailSinDistinguirMayusculas() {
            autenticar("Laura.Mendoza@ESCUELA.com", Rol.MAESTRO);

            assertThatCode(() -> currentUserService.validarMaestroDeMateria(materia))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rechaza que un maestro registre en una materia que no imparte")
        void maestroNoRegistraEnMateriaAjena() {
            autenticar("jorge.salas@escuela.com", Rol.MAESTRO);

            assertThatThrownBy(() -> currentUserService.validarMaestroDeMateria(materia))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("sus propias materias");
        }

        @Test
        @DisplayName("no restringe a ADMIN")
        void adminNoTieneRestriccion() {
            autenticar("admin@escuela.com", Rol.ADMIN);

            assertThatCode(() -> currentUserService.validarMaestroDeMateria(materia))
                    .doesNotThrowAnyException();
        }
    }
}
