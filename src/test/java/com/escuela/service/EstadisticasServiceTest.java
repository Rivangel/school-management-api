package com.escuela.service;

import com.escuela.model.dto.AsistenciaMateriaResponse;
import com.escuela.model.dto.AsistenciaResumenResponse;
import com.escuela.model.dto.EstadisticasResponse;
import com.escuela.model.dto.PromedioMateriaResponse;
import com.escuela.model.entity.Alumno;
import com.escuela.model.entity.Asistencia;
import com.escuela.model.entity.Calificacion;
import com.escuela.model.entity.Maestro;
import com.escuela.model.entity.Materia;
import com.escuela.repository.AlumnoRepository;
import com.escuela.repository.AsistenciaRepository;
import com.escuela.repository.CalificacionRepository;
import com.escuela.repository.MaestroRepository;
import com.escuela.repository.MateriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstadisticasService")
class EstadisticasServiceTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private MaestroRepository maestroRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private CalificacionRepository calificacionRepository;

    @Mock
    private AsistenciaRepository asistenciaRepository;

    @InjectMocks
    private EstadisticasService estadisticasService;

    private Materia matematicas;
    private Materia poo;
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        Maestro maestro = new Maestro("Juan", "Pérez", "juan.perez@escuela.com", "Matemáticas");
        maestro.setId(1L);
        matematicas = new Materia("Matemáticas I", 8, maestro);
        matematicas.setId(1L);
        poo = new Materia("Programación Orientada a Objetos", 10, maestro);
        poo.setId(2L);

        alumno = new Alumno("Ana", "López", "A2026001", "ana.lopez@escuela.com", "A");
        alumno.setId(1L);

        when(alumnoRepository.count()).thenReturn(3L);
        when(maestroRepository.count()).thenReturn(2L);
        when(materiaRepository.count()).thenReturn(3L);
    }

    private Calificacion calificacion(Materia materia, String nota) {
        return new Calificacion(alumno, materia, new BigDecimal(nota), "2026-1");
    }

    private Asistencia asistencia(Materia materia, boolean presente) {
        return new Asistencia(alumno, materia, LocalDate.of(2026, 7, 20), presente);
    }

    @Nested
    @DisplayName("conteos")
    class Conteos {

        @Test
        @DisplayName("refleja el total de alumnos, maestros y materias")
        void devuelveLosConteos() {
            when(calificacionRepository.findAll()).thenReturn(List.of());
            when(asistenciaRepository.findAll()).thenReturn(List.of());

            EstadisticasResponse resultado = estadisticasService.obtener();

            assertThat(resultado.totalAlumnos()).isEqualTo(3L);
            assertThat(resultado.totalMaestros()).isEqualTo(2L);
            assertThat(resultado.totalMaterias()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("promedio de calificaciones")
    class Promedios {

        @Test
        @DisplayName("es null cuando no hay ninguna calificación registrada")
        void sinCalificacionesEsNull() {
            when(calificacionRepository.findAll()).thenReturn(List.of());
            when(asistenciaRepository.findAll()).thenReturn(List.of());

            EstadisticasResponse resultado = estadisticasService.obtener();

            assertThat(resultado.promedioGeneral()).isNull();
            assertThat(resultado.promedioPorMateria()).isEmpty();
        }

        @Test
        @DisplayName("promedia todas las notas y redondea a dos decimales")
        void promedioGeneralRedondeado() {
            when(calificacionRepository.findAll()).thenReturn(List.of(
                    calificacion(matematicas, "9.50"),
                    calificacion(matematicas, "7.20"),
                    calificacion(poo, "8.70")));
            when(asistenciaRepository.findAll()).thenReturn(List.of());

            EstadisticasResponse resultado = estadisticasService.obtener();

            // (9.50 + 7.20 + 8.70) / 3 = 8.4666... -> 8.47
            assertThat(resultado.promedioGeneral()).isEqualTo(8.47);
        }

        @Test
        @DisplayName("agrupa por materia, cada una con su propio promedio y conteo")
        void agrupaPorMateria() {
            when(calificacionRepository.findAll()).thenReturn(List.of(
                    calificacion(matematicas, "9.50"),
                    calificacion(matematicas, "7.20"),
                    calificacion(poo, "8.70")));
            when(asistenciaRepository.findAll()).thenReturn(List.of());

            EstadisticasResponse resultado = estadisticasService.obtener();

            assertThat(resultado.promedioPorMateria())
                    .extracting(PromedioMateriaResponse::materiaId,
                            PromedioMateriaResponse::materiaNombre,
                            PromedioMateriaResponse::promedio,
                            PromedioMateriaResponse::cantidadCalificaciones)
                    .containsExactly(
                            tuple(1L, "Matemáticas I", 8.35, 2L),
                            tuple(2L, "Programación Orientada a Objetos", 8.70, 1L));
        }

        @Test
        @DisplayName("una materia sin calificar no aparece en la lista")
        void materiaSinCalificarNoAparece() {
            when(calificacionRepository.findAll()).thenReturn(List.of(calificacion(matematicas, "9.50")));
            when(asistenciaRepository.findAll()).thenReturn(List.of());

            EstadisticasResponse resultado = estadisticasService.obtener();

            assertThat(resultado.promedioPorMateria())
                    .extracting(PromedioMateriaResponse::materiaId)
                    .containsExactly(1L);
        }
    }

    @Nested
    @DisplayName("asistencia")
    class AsistenciaEstadisticas {

        @Test
        @DisplayName("el resumen general es null cuando no hay ningún registro")
        void sinRegistrosElResumenEsNull() {
            when(calificacionRepository.findAll()).thenReturn(List.of());
            when(asistenciaRepository.findAll()).thenReturn(List.of());

            EstadisticasResponse resultado = estadisticasService.obtener();

            assertThat(resultado.asistenciaGeneral())
                    .isEqualTo(new AsistenciaResumenResponse(0, 0, 0, null));
            assertThat(resultado.asistenciaPorMateria()).isEmpty();
        }

        @Test
        @DisplayName("cuenta presentes/ausentes y calcula el porcentaje general")
        void resumenGeneral() {
            when(calificacionRepository.findAll()).thenReturn(List.of());
            when(asistenciaRepository.findAll()).thenReturn(List.of(
                    asistencia(matematicas, true),
                    asistencia(matematicas, true),
                    asistencia(poo, false)));

            EstadisticasResponse resultado = estadisticasService.obtener();

            // 2 de 3 presentes = 66.666... -> 66.67
            assertThat(resultado.asistenciaGeneral())
                    .isEqualTo(new AsistenciaResumenResponse(2, 1, 3, 66.67));
        }

        @Test
        @DisplayName("agrupa por materia con su propio porcentaje")
        void agrupaPorMateria() {
            when(calificacionRepository.findAll()).thenReturn(List.of());
            when(asistenciaRepository.findAll()).thenReturn(List.of(
                    asistencia(matematicas, true),
                    asistencia(matematicas, true),
                    asistencia(matematicas, false),
                    asistencia(poo, false)));

            EstadisticasResponse resultado = estadisticasService.obtener();

            assertThat(resultado.asistenciaPorMateria())
                    .extracting(AsistenciaMateriaResponse::materiaId,
                            AsistenciaMateriaResponse::presentes,
                            AsistenciaMateriaResponse::total,
                            AsistenciaMateriaResponse::porcentaje)
                    .containsExactly(
                            tuple(1L, 2L, 3L, 66.67),
                            tuple(2L, 0L, 1L, 0.0));
        }
    }
}
