package com.escuela.service;

import com.escuela.exception.BadRequestException;
import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.dto.AlumnoRequest;
import com.escuela.model.dto.AlumnoResponse;
import com.escuela.model.entity.Alumno;
import com.escuela.repository.AlumnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlumnoService")
class AlumnoServiceTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private AlumnoService alumnoService;

    private Alumno alumno;
    private AlumnoRequest request;

    @BeforeEach
    void setUp() {
        alumno = new Alumno("Ana", "López", "A2026001", "ana.lopez@escuela.com", "A");
        alumno.setId(1L);
        request = new AlumnoRequest("Ana", "López", "A2026001", "ana.lopez@escuela.com", "A");
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("devuelve todos los alumnos mapeados a AlumnoResponse")
        void devuelveTodosLosAlumnos() {
            Alumno otro = new Alumno("Carlos", "Ramírez", "A2026002", "carlos.ramirez@escuela.com", "B");
            otro.setId(2L);
            when(alumnoRepository.findAll()).thenReturn(List.of(alumno, otro));

            List<AlumnoResponse> resultado = alumnoService.listar();

            assertThat(resultado).hasSize(2);
            assertThat(resultado).extracting(AlumnoResponse::matricula)
                    .containsExactly("A2026001", "A2026002");
            assertThat(resultado.get(0).nombre()).isEqualTo("Ana");
        }

        @Test
        @DisplayName("devuelve una lista vacía cuando no hay alumnos")
        void devuelveListaVacia() {
            when(alumnoRepository.findAll()).thenReturn(List.of());

            assertThat(alumnoService.listar()).isEmpty();
        }
    }

    @Nested
    @DisplayName("obtenerPorId")
    class ObtenerPorId {

        @Test
        @DisplayName("devuelve el alumno cuando existe")
        void devuelveAlumnoExistente() {
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));

            AlumnoResponse resultado = alumnoService.obtenerPorId(1L);

            assertThat(resultado.id()).isEqualTo(1L);
            assertThat(resultado.email()).isEqualTo("ana.lopez@escuela.com");
            assertThat(resultado.grupo()).isEqualTo("A");
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el id no existe")
        void lanzaExcepcionCuandoNoExiste() {
            when(alumnoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alumnoService.obtenerPorId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Alumno")
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("guarda el alumno cuando matrícula y email son únicos")
        void creaAlumno() {
            when(alumnoRepository.existsByMatricula("A2026001")).thenReturn(false);
            when(alumnoRepository.existsByEmail("ana.lopez@escuela.com")).thenReturn(false);
            when(alumnoRepository.save(any(Alumno.class))).thenReturn(alumno);

            AlumnoResponse resultado = alumnoService.crear(request);

            ArgumentCaptor<Alumno> captor = ArgumentCaptor.forClass(Alumno.class);
            verify(alumnoRepository).save(captor.capture());
            Alumno guardado = captor.getValue();
            assertThat(guardado.getNombre()).isEqualTo("Ana");
            assertThat(guardado.getApellido()).isEqualTo("López");
            assertThat(guardado.getMatricula()).isEqualTo("A2026001");
            assertThat(guardado.getEmail()).isEqualTo("ana.lopez@escuela.com");
            assertThat(guardado.getGrupo()).isEqualTo("A");
            assertThat(resultado.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("rechaza matrículas duplicadas")
        void rechazaMatriculaDuplicada() {
            when(alumnoRepository.existsByMatricula("A2026001")).thenReturn(true);

            assertThatThrownBy(() -> alumnoService.crear(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("A2026001");

            verify(alumnoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza emails duplicados")
        void rechazaEmailDuplicado() {
            when(alumnoRepository.existsByMatricula("A2026001")).thenReturn(false);
            when(alumnoRepository.existsByEmail("ana.lopez@escuela.com")).thenReturn(true);

            assertThatThrownBy(() -> alumnoService.crear(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("ana.lopez@escuela.com");

            verify(alumnoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("actualizar")
    class Actualizar {

        @Test
        @DisplayName("actualiza todos los campos del alumno")
        void actualizaAlumno() {
            AlumnoRequest cambios = new AlumnoRequest(
                    "Ana María", "López Ruiz", "A2026099", "ana.maria@escuela.com", "C");
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(alumnoRepository.existsByMatricula("A2026099")).thenReturn(false);
            when(alumnoRepository.existsByEmail("ana.maria@escuela.com")).thenReturn(false);
            when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

            AlumnoResponse resultado = alumnoService.actualizar(1L, cambios);

            assertThat(resultado.nombre()).isEqualTo("Ana María");
            assertThat(resultado.apellido()).isEqualTo("López Ruiz");
            assertThat(resultado.matricula()).isEqualTo("A2026099");
            assertThat(resultado.email()).isEqualTo("ana.maria@escuela.com");
            assertThat(resultado.grupo()).isEqualTo("C");
        }

        @Test
        @DisplayName("permite conservar la misma matrícula y el mismo email")
        void permiteConservarSusPropiosValoresUnicos() {
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

            AlumnoResponse resultado = alumnoService.actualizar(1L, request);

            assertThat(resultado.matricula()).isEqualTo("A2026001");
            verify(alumnoRepository, never()).existsByMatricula(any());
            verify(alumnoRepository, never()).existsByEmail(any());
        }

        @Test
        @DisplayName("rechaza una matrícula que ya pertenece a otro alumno")
        void rechazaMatriculaDeOtroAlumno() {
            AlumnoRequest cambios = new AlumnoRequest(
                    "Ana", "López", "A2026002", "ana.lopez@escuela.com", "A");
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(alumnoRepository.existsByMatricula("A2026002")).thenReturn(true);

            assertThatThrownBy(() -> alumnoService.actualizar(1L, cambios))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("A2026002");

            verify(alumnoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza un email que ya pertenece a otro alumno")
        void rechazaEmailDeOtroAlumno() {
            AlumnoRequest cambios = new AlumnoRequest(
                    "Ana", "López", "A2026001", "carlos.ramirez@escuela.com", "A");
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(alumnoRepository.existsByEmail("carlos.ramirez@escuela.com")).thenReturn(true);

            assertThatThrownBy(() -> alumnoService.actualizar(1L, cambios))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("carlos.ramirez@escuela.com");

            verify(alumnoRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el alumno no existe")
        void lanzaExcepcionCuandoNoExiste() {
            when(alumnoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alumnoService.actualizar(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(alumnoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        @DisplayName("elimina el alumno cuando existe")
        void eliminaAlumno() {
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));

            alumnoService.eliminar(1L);

            verify(alumnoRepository).delete(alumno);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el alumno no existe")
        void lanzaExcepcionCuandoNoExiste() {
            when(alumnoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alumnoService.eliminar(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(alumnoRepository, never()).delete(any());
        }
    }
}
