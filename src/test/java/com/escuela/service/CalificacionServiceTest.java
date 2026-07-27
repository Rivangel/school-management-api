package com.escuela.service;

import com.escuela.exception.OperacionNoPermitidaException;
import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.dto.CalificacionRequest;
import com.escuela.model.dto.CalificacionResponse;
import com.escuela.model.entity.Alumno;
import com.escuela.model.entity.Calificacion;
import com.escuela.model.entity.Maestro;
import com.escuela.model.entity.Materia;
import com.escuela.repository.AlumnoRepository;
import com.escuela.repository.CalificacionRepository;
import com.escuela.repository.MateriaRepository;
import com.escuela.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalificacionService")
class CalificacionServiceTest {

    @Mock
    private CalificacionRepository calificacionRepository;

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private CalificacionService calificacionService;

    private Alumno alumno;
    private Materia materia;
    private CalificacionRequest request;

    @BeforeEach
    void setUp() {
        alumno = new Alumno("Ana", "Torres", "A-2026-001", "ana.torres@escuela.com", "3A");
        alumno.setId(1L);
        Maestro maestro = new Maestro("Laura", "Mendoza", "laura.mendoza@escuela.com", "Matemáticas");
        maestro.setId(5L);
        materia = new Materia("Álgebra I", 8, maestro);
        materia.setId(10L);
        request = new CalificacionRequest(1L, 10L, new BigDecimal("9.50"), "2026-1");
    }

    private Calificacion calificacionExistente(String valor) {
        Calificacion existente = new Calificacion(alumno, materia, new BigDecimal(valor), "2026-1");
        existente.setId(100L);
        return existente;
    }

    @Nested
    @DisplayName("registrar")
    class Registrar {

        @Test
        @DisplayName("crea una calificación nueva cuando no existe una previa del mismo periodo")
        void creaCalificacionNueva() {
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));
            when(calificacionRepository.findByAlumnoIdAndMateriaIdAndPeriodo(1L, 10L, "2026-1"))
                    .thenReturn(Optional.empty());
            when(calificacionRepository.save(any(Calificacion.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CalificacionResponse resultado = calificacionService.registrar(request);

            ArgumentCaptor<Calificacion> captor = ArgumentCaptor.forClass(Calificacion.class);
            verify(calificacionRepository).save(captor.capture());
            Calificacion guardada = captor.getValue();
            assertThat(guardada.getId()).isNull();
            assertThat(guardada.getAlumno()).isSameAs(alumno);
            assertThat(guardada.getMateria()).isSameAs(materia);
            assertThat(guardada.getCalificacion()).isEqualByComparingTo("9.50");
            assertThat(guardada.getPeriodo()).isEqualTo("2026-1");

            assertThat(resultado.alumnoId()).isEqualTo(1L);
            assertThat(resultado.alumnoNombre()).isEqualTo("Ana Torres");
            assertThat(resultado.materiaId()).isEqualTo(10L);
            assertThat(resultado.materiaNombre()).isEqualTo("Álgebra I");
            assertThat(resultado.periodo()).isEqualTo("2026-1");
        }

        @Test
        @DisplayName("actualiza la calificación existente del mismo alumno, materia y periodo (upsert)")
        void actualizaCalificacionExistente() {
            Calificacion existente = calificacionExistente("7.00");
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));
            when(calificacionRepository.findByAlumnoIdAndMateriaIdAndPeriodo(1L, 10L, "2026-1"))
                    .thenReturn(Optional.of(existente));
            when(calificacionRepository.save(any(Calificacion.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CalificacionResponse resultado = calificacionService.registrar(request);

            ArgumentCaptor<Calificacion> captor = ArgumentCaptor.forClass(Calificacion.class);
            verify(calificacionRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existente);
            assertThat(existente.getCalificacion()).isEqualByComparingTo("9.50");
            assertThat(resultado.id()).isEqualTo(100L);
            assertThat(resultado.calificacion()).isEqualByComparingTo("9.50");
        }

        @Test
        @DisplayName("distingue el periodo: otro periodo genera un registro nuevo")
        void otroPeriodoCreaRegistroNuevo() {
            CalificacionRequest segundoPeriodo =
                    new CalificacionRequest(1L, 10L, new BigDecimal("8.00"), "2026-2");
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));
            when(calificacionRepository.findByAlumnoIdAndMateriaIdAndPeriodo(1L, 10L, "2026-2"))
                    .thenReturn(Optional.empty());
            when(calificacionRepository.save(any(Calificacion.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CalificacionResponse resultado = calificacionService.registrar(segundoPeriodo);

            assertThat(resultado.id()).isNull();
            assertThat(resultado.periodo()).isEqualTo("2026-2");
            assertThat(resultado.calificacion()).isEqualByComparingTo("8.00");
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el alumno no existe")
        void rechazaAlumnoInexistente() {
            CalificacionRequest sinAlumno =
                    new CalificacionRequest(404L, 10L, new BigDecimal("9.50"), "2026-1");
            when(alumnoRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> calificacionService.registrar(sinAlumno))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Alumno")
                    .hasMessageContaining("404");

            verify(calificacionRepository, never()).save(any());
            verifyNoInteractions(currentUserService);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando la materia no existe")
        void rechazaMateriaInexistente() {
            CalificacionRequest sinMateria =
                    new CalificacionRequest(1L, 404L, new BigDecimal("9.50"), "2026-1");
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(materiaRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> calificacionService.registrar(sinMateria))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Materia")
                    .hasMessageContaining("404");

            verify(calificacionRepository, never()).save(any());
            verifyNoInteractions(currentUserService);
        }

        @Test
        @DisplayName("propaga OperacionNoPermitidaException si la materia no es del maestro autenticado")
        void rechazaMaestroAjeno() {
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));
            doThrow(new OperacionNoPermitidaException(
                    "Un maestro solo puede registrar información de sus propias materias"))
                    .when(currentUserService).validarMaestroDeMateria(materia);

            assertThatThrownBy(() -> calificacionService.registrar(request))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("sus propias materias");

            verify(calificacionRepository, never())
                    .findByAlumnoIdAndMateriaIdAndPeriodo(anyLong(), anyLong(), anyString());
            verify(calificacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("valida la propiedad de la materia antes de consultar o guardar")
        void validaPropiedadAntesDeGuardar() {
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));
            when(calificacionRepository.findByAlumnoIdAndMateriaIdAndPeriodo(1L, 10L, "2026-1"))
                    .thenReturn(Optional.empty());
            when(calificacionRepository.save(any(Calificacion.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            calificacionService.registrar(request);

            verify(currentUserService).validarMaestroDeMateria(materia);
        }
    }

    @Nested
    @DisplayName("listarPorAlumno")
    class ListarPorAlumno {

        @Test
        @DisplayName("devuelve las calificaciones del alumno mapeadas a CalificacionResponse")
        void devuelveCalificacionesDelAlumno() {
            Calificacion primera = calificacionExistente("7.00");
            Calificacion segunda = new Calificacion(alumno, materia, new BigDecimal("8.50"), "2026-2");
            segunda.setId(101L);
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(calificacionRepository.findByAlumnoId(1L)).thenReturn(List.of(primera, segunda));

            List<CalificacionResponse> resultado = calificacionService.listarPorAlumno(1L);

            assertThat(resultado).hasSize(2);
            assertThat(resultado).extracting(CalificacionResponse::periodo)
                    .containsExactly("2026-1", "2026-2");
            assertThat(resultado.get(0).alumnoNombre()).isEqualTo("Ana Torres");
            assertThat(resultado.get(0).materiaNombre()).isEqualTo("Álgebra I");
        }

        @Test
        @DisplayName("devuelve una lista vacía cuando el alumno no tiene calificaciones")
        void devuelveListaVacia() {
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(calificacionRepository.findByAlumnoId(1L)).thenReturn(List.of());

            assertThat(calificacionService.listarPorAlumno(1L)).isEmpty();
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el alumno no existe")
        void lanzaExcepcionCuandoElAlumnoNoExiste() {
            when(alumnoRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> calificacionService.listarPorAlumno(404L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Alumno");

            verify(calificacionRepository, never()).findByAlumnoId(anyLong());
        }

        @Test
        @DisplayName("propaga OperacionNoPermitidaException si un alumno consulta datos ajenos")
        void rechazaAlumnoAjeno() {
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            doThrow(new OperacionNoPermitidaException(
                    "Un alumno solo puede consultar su propia información"))
                    .when(currentUserService).validarAccesoAlumno(alumno);

            assertThatThrownBy(() -> calificacionService.listarPorAlumno(1L))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("su propia información");

            verify(calificacionRepository, never()).findByAlumnoId(anyLong());
        }
    }

    @Nested
    @DisplayName("listarPorMateria")
    class ListarPorMateria {

        @Test
        @DisplayName("devuelve las calificaciones de la materia")
        void devuelveCalificacionesDeLaMateria() {
            when(materiaRepository.existsById(10L)).thenReturn(true);
            when(calificacionRepository.findByMateriaId(10L))
                    .thenReturn(List.of(calificacionExistente("7.00")));

            List<CalificacionResponse> resultado = calificacionService.listarPorMateria(10L);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).materiaId()).isEqualTo(10L);
            assertThat(resultado.get(0).calificacion()).isEqualByComparingTo("7.00");
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando la materia no existe")
        void lanzaExcepcionCuandoLaMateriaNoExiste() {
            when(materiaRepository.existsById(404L)).thenReturn(false);

            assertThatThrownBy(() -> calificacionService.listarPorMateria(404L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Materia")
                    .hasMessageContaining("404");

            verify(calificacionRepository, never()).findByMateriaId(anyLong());
        }
    }
}
