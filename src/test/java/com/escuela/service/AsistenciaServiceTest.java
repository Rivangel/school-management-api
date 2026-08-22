package com.escuela.service;

import com.escuela.exception.OperacionNoPermitidaException;
import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.dto.AsistenciaRequest;
import com.escuela.model.dto.AsistenciaResponse;
import com.escuela.model.entity.Alumno;
import com.escuela.model.entity.Asistencia;
import com.escuela.model.entity.Maestro;
import com.escuela.model.entity.Materia;
import com.escuela.repository.AlumnoRepository;
import com.escuela.repository.AsistenciaRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsistenciaService")
class AsistenciaServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 10);

    @Mock
    private AsistenciaRepository asistenciaRepository;

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AsistenciaService asistenciaService;

    private Alumno alumno;
    private Materia materia;
    private AsistenciaRequest request;

    @BeforeEach
    void setUp() {
        alumno = new Alumno("Ana", "Torres", "A-2026-001", "ana.torres@escuela.com", "3A");
        alumno.setId(1L);
        Maestro maestro = new Maestro("Laura", "Mendoza", "laura.mendoza@escuela.com", "Matemáticas");
        maestro.setId(5L);
        materia = new Materia("Álgebra I", 8, maestro);
        materia.setId(10L);
        request = new AsistenciaRequest(1L, 10L, FECHA, true);
    }

    private Asistencia asistenciaExistente(boolean presente) {
        Asistencia existente = new Asistencia(alumno, materia, FECHA, presente);
        existente.setId(200L);
        return existente;
    }

    @Nested
    @DisplayName("registrar")
    class Registrar {

        @Test
        @DisplayName("crea un registro nuevo cuando no hay asistencia previa de esa fecha")
        void creaAsistenciaNueva() {
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));
            when(asistenciaRepository.findByAlumnoIdAndMateriaIdAndFecha(1L, 10L, FECHA))
                    .thenReturn(Optional.empty());
            when(asistenciaRepository.save(any(Asistencia.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AsistenciaResponse resultado = asistenciaService.registrar(request);

            ArgumentCaptor<Asistencia> captor = ArgumentCaptor.forClass(Asistencia.class);
            verify(asistenciaRepository).save(captor.capture());
            Asistencia guardada = captor.getValue();
            assertThat(guardada.getId()).isNull();
            assertThat(guardada.getAlumno()).isSameAs(alumno);
            assertThat(guardada.getMateria()).isSameAs(materia);
            assertThat(guardada.getFecha()).isEqualTo(FECHA);
            assertThat(guardada.getPresente()).isTrue();

            assertThat(resultado.alumnoNombre()).isEqualTo("Ana Torres");
            assertThat(resultado.materiaNombre()).isEqualTo("Álgebra I");
            assertThat(resultado.fecha()).isEqualTo(FECHA);
            assertThat(resultado.presente()).isTrue();
        }

        @Test
        @DisplayName("corrige el registro existente del mismo alumno, materia y fecha (upsert)")
        void actualizaAsistenciaExistente() {
            Asistencia existente = asistenciaExistente(false);
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));
            when(asistenciaRepository.findByAlumnoIdAndMateriaIdAndFecha(1L, 10L, FECHA))
                    .thenReturn(Optional.of(existente));
            when(asistenciaRepository.save(any(Asistencia.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AsistenciaResponse resultado = asistenciaService.registrar(request);

            ArgumentCaptor<Asistencia> captor = ArgumentCaptor.forClass(Asistencia.class);
            verify(asistenciaRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existente);
            assertThat(existente.getPresente()).isTrue();
            assertThat(resultado.id()).isEqualTo(200L);
            assertThat(resultado.presente()).isTrue();
        }

        @Test
        @DisplayName("distingue la fecha: otro día genera un registro nuevo")
        void otraFechaCreaRegistroNuevo() {
            LocalDate otroDia = FECHA.plusDays(1);
            AsistenciaRequest siguienteClase = new AsistenciaRequest(1L, 10L, otroDia, false);
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));
            when(asistenciaRepository.findByAlumnoIdAndMateriaIdAndFecha(1L, 10L, otroDia))
                    .thenReturn(Optional.empty());
            when(asistenciaRepository.save(any(Asistencia.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AsistenciaResponse resultado = asistenciaService.registrar(siguienteClase);

            assertThat(resultado.id()).isNull();
            assertThat(resultado.fecha()).isEqualTo(otroDia);
            assertThat(resultado.presente()).isFalse();
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el alumno no existe")
        void rechazaAlumnoInexistente() {
            AsistenciaRequest sinAlumno = new AsistenciaRequest(404L, 10L, FECHA, true);
            when(alumnoRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> asistenciaService.registrar(sinAlumno))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Alumno")
                    .hasMessageContaining("404");

            verify(asistenciaRepository, never()).save(any());
            verifyNoInteractions(currentUserService);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando la materia no existe")
        void rechazaMateriaInexistente() {
            AsistenciaRequest sinMateria = new AsistenciaRequest(1L, 404L, FECHA, true);
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(materiaRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> asistenciaService.registrar(sinMateria))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Materia")
                    .hasMessageContaining("404");

            verify(asistenciaRepository, never()).save(any());
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

            assertThatThrownBy(() -> asistenciaService.registrar(request))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("sus propias materias");

            verify(asistenciaRepository, never())
                    .findByAlumnoIdAndMateriaIdAndFecha(anyLong(), anyLong(), any());
            verify(asistenciaRepository, never()).save(any());
        }

        @Test
        @DisplayName("valida la propiedad de la materia antes de consultar o guardar")
        void validaPropiedadAntesDeGuardar() {
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));
            when(asistenciaRepository.findByAlumnoIdAndMateriaIdAndFecha(1L, 10L, FECHA))
                    .thenReturn(Optional.empty());
            when(asistenciaRepository.save(any(Asistencia.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            asistenciaService.registrar(request);

            verify(currentUserService).validarMaestroDeMateria(materia);
        }
    }

    @Nested
    @DisplayName("listarPorAlumno")
    class ListarPorAlumno {

        @Test
        @DisplayName("devuelve las asistencias del alumno mapeadas a AsistenciaResponse")
        void devuelveAsistenciasDelAlumno() {
            Asistencia primera = asistenciaExistente(true);
            Asistencia segunda = new Asistencia(alumno, materia, FECHA.plusDays(1), false);
            segunda.setId(201L);
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(asistenciaRepository.findByAlumnoId(1L)).thenReturn(List.of(primera, segunda));

            List<AsistenciaResponse> resultado = asistenciaService.listarPorAlumno(1L);

            assertThat(resultado).hasSize(2);
            assertThat(resultado).extracting(AsistenciaResponse::presente)
                    .containsExactly(true, false);
            assertThat(resultado.get(0).alumnoNombre()).isEqualTo("Ana Torres");
            assertThat(resultado.get(1).fecha()).isEqualTo(FECHA.plusDays(1));
        }

        @Test
        @DisplayName("devuelve una lista vacía cuando el alumno no tiene registros")
        void devuelveListaVacia() {
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            when(asistenciaRepository.findByAlumnoId(1L)).thenReturn(List.of());

            assertThat(asistenciaService.listarPorAlumno(1L)).isEmpty();
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el alumno no existe")
        void lanzaExcepcionCuandoElAlumnoNoExiste() {
            when(alumnoRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> asistenciaService.listarPorAlumno(404L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Alumno");

            verify(asistenciaRepository, never()).findByAlumnoId(anyLong());
        }

        @Test
        @DisplayName("propaga OperacionNoPermitidaException si un alumno consulta datos ajenos")
        void rechazaAlumnoAjeno() {
            when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
            doThrow(new OperacionNoPermitidaException(
                    "Un alumno solo puede consultar su propia información"))
                    .when(currentUserService).validarAccesoAlumno(alumno);

            assertThatThrownBy(() -> asistenciaService.listarPorAlumno(1L))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("su propia información");

            verify(asistenciaRepository, never()).findByAlumnoId(anyLong());
        }
    }

    @Nested
    @DisplayName("listarPorMateriaYFecha")
    class ListarPorMateriaYFecha {

        @Test
        @DisplayName("devuelve lo ya registrado de esa materia ese día")
        void devuelveLoRegistradoDelDia() {
            // Es lo que necesita saber quien va a pasar lista: sin esto, la única forma
            // de averiguarlo era preguntar alumno por alumno.
            when(materiaRepository.existsById(10L)).thenReturn(true);
            when(asistenciaRepository.findByMateriaIdAndFecha(10L, FECHA))
                    .thenReturn(List.of(asistenciaExistente(true)));

            List<AsistenciaResponse> respuesta =
                    asistenciaService.listarPorMateriaYFecha(10L, FECHA);

            assertThat(respuesta).hasSize(1);
            assertThat(respuesta.get(0).alumnoId()).isEqualTo(1L);
            assertThat(respuesta.get(0).presente()).isTrue();
        }

        @Test
        @DisplayName("un día sin lista pasada devuelve vacío, no un 404")
        void diaSinRegistrosDevuelveVacio() {
            // No encontrar nada es la respuesta normal: casi siempre se pasa lista de un
            // día que todavía no la tiene.
            when(materiaRepository.existsById(10L)).thenReturn(true);
            when(asistenciaRepository.findByMateriaIdAndFecha(10L, FECHA)).thenReturn(List.of());

            assertThat(asistenciaService.listarPorMateriaYFecha(10L, FECHA)).isEmpty();
        }

        @Test
        @DisplayName("una materia que no existe sí es un 404")
        void materiaInexistenteLanzaNotFound() {
            // Aquí la materia es el recurso, no un filtro: distinguirlo importa.
            when(materiaRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> asistenciaService.listarPorMateriaYFecha(99L, FECHA))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(asistenciaRepository, never()).findByMateriaIdAndFecha(anyLong(), any());
        }

        @Test
        @DisplayName("leerla no exige ser el maestro de la materia")
        void leerNoExigeSerElMaestro() {
            // Igual que en calificaciones: registrar sí lo exige, consultar no.
            when(materiaRepository.existsById(10L)).thenReturn(true);
            when(asistenciaRepository.findByMateriaIdAndFecha(10L, FECHA)).thenReturn(List.of());

            asistenciaService.listarPorMateriaYFecha(10L, FECHA);

            verifyNoInteractions(currentUserService);
        }
    }
}
