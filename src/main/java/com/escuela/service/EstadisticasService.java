package com.escuela.service;

import com.escuela.model.dto.AsistenciaMateriaResponse;
import com.escuela.model.dto.AsistenciaResumenResponse;
import com.escuela.model.dto.EstadisticasResponse;
import com.escuela.model.dto.PromedioMateriaResponse;
import com.escuela.model.entity.Asistencia;
import com.escuela.model.entity.Calificacion;
import com.escuela.repository.AlumnoRepository;
import com.escuela.repository.AsistenciaRepository;
import com.escuela.repository.CalificacionRepository;
import com.escuela.repository.MaestroRepository;
import com.escuela.repository.MateriaRepository;
import com.escuela.util.PdfGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Indicadores agregados para el dashboard: conteos, promedio de calificaciones
 * y porcentaje de asistencia, generales y por materia.
 *
 * <p>Se calcula recorriendo {@code findAll()} en memoria, como ya hace
 * {@link PdfGenerator} con el promedio de una boleta, en vez de agregarlo con
 * SQL: el volumen de datos de la escuela no lo justifica, y así el cálculo se
 * prueba con Mockito como el resto de los servicios, sin depender de una base
 * de datos real.
 */
@Service
public class EstadisticasService {

    private final AlumnoRepository alumnoRepository;
    private final MaestroRepository maestroRepository;
    private final MateriaRepository materiaRepository;
    private final CalificacionRepository calificacionRepository;
    private final AsistenciaRepository asistenciaRepository;

    public EstadisticasService(AlumnoRepository alumnoRepository,
                               MaestroRepository maestroRepository,
                               MateriaRepository materiaRepository,
                               CalificacionRepository calificacionRepository,
                               AsistenciaRepository asistenciaRepository) {
        this.alumnoRepository = alumnoRepository;
        this.maestroRepository = maestroRepository;
        this.materiaRepository = materiaRepository;
        this.calificacionRepository = calificacionRepository;
        this.asistenciaRepository = asistenciaRepository;
    }

    @Transactional(readOnly = true)
    public EstadisticasResponse obtener() {
        List<Calificacion> calificaciones = calificacionRepository.findAll();
        List<Asistencia> asistencias = asistenciaRepository.findAll();

        return new EstadisticasResponse(
                alumnoRepository.count(),
                maestroRepository.count(),
                materiaRepository.count(),
                promedio(calificaciones.stream().map(Calificacion::getCalificacion).toList()),
                promedioPorMateria(calificaciones),
                resumenAsistencia(asistencias),
                asistenciaPorMateria(asistencias)
        );
    }

    private List<PromedioMateriaResponse> promedioPorMateria(List<Calificacion> calificaciones) {
        return calificaciones.stream()
                .collect(Collectors.groupingBy(c -> c.getMateria().getId()))
                .values().stream()
                .map(grupo -> new PromedioMateriaResponse(
                        grupo.get(0).getMateria().getId(),
                        grupo.get(0).getMateria().getNombre(),
                        promedio(grupo.stream().map(Calificacion::getCalificacion).toList()),
                        grupo.size()))
                .sorted(Comparator.comparing(PromedioMateriaResponse::materiaNombre))
                .toList();
    }

    private AsistenciaResumenResponse resumenAsistencia(List<Asistencia> asistencias) {
        long total = asistencias.size();
        long presentes = asistencias.stream().filter(Asistencia::getPresente).count();
        return new AsistenciaResumenResponse(presentes, total - presentes, total, porcentaje(presentes, total));
    }

    private List<AsistenciaMateriaResponse> asistenciaPorMateria(List<Asistencia> asistencias) {
        return asistencias.stream()
                .collect(Collectors.groupingBy(a -> a.getMateria().getId()))
                .values().stream()
                .map(grupo -> {
                    long total = grupo.size();
                    long presentes = grupo.stream().filter(Asistencia::getPresente).count();
                    return new AsistenciaMateriaResponse(
                            grupo.get(0).getMateria().getId(),
                            grupo.get(0).getMateria().getNombre(),
                            presentes,
                            total,
                            porcentaje(presentes, total));
                })
                .sorted(Comparator.comparing(AsistenciaMateriaResponse::materiaNombre))
                .toList();
    }

    /** {@code null} si la lista viene vacía: sin notas no hay promedio que mostrar. */
    private static Double promedio(List<BigDecimal> notas) {
        if (notas.isEmpty()) {
            return null;
        }
        BigDecimal suma = notas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(notas.size()), 2, RoundingMode.HALF_UP).doubleValue();
    }

    /** {@code null} si {@code total} es 0: evita la división entre cero y un 0% engañoso. */
    private static Double porcentaje(long presentes, long total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(presentes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
