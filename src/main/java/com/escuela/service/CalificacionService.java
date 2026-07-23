package com.escuela.service;

import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.dto.CalificacionRequest;
import com.escuela.model.dto.CalificacionResponse;
import com.escuela.model.entity.Alumno;
import com.escuela.model.entity.Calificacion;
import com.escuela.model.entity.Materia;
import com.escuela.repository.AlumnoRepository;
import com.escuela.repository.CalificacionRepository;
import com.escuela.repository.MateriaRepository;
import com.escuela.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final AlumnoRepository alumnoRepository;
    private final MateriaRepository materiaRepository;
    private final CurrentUserService currentUserService;

    public CalificacionService(CalificacionRepository calificacionRepository,
                               AlumnoRepository alumnoRepository,
                               MateriaRepository materiaRepository,
                               CurrentUserService currentUserService) {
        this.calificacionRepository = calificacionRepository;
        this.alumnoRepository = alumnoRepository;
        this.materiaRepository = materiaRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Registra una calificación. Si ya existe una para el mismo alumno,
     * materia y periodo, se actualiza su valor.
     */
    @Transactional
    public CalificacionResponse registrar(CalificacionRequest request) {
        Alumno alumno = alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno", request.alumnoId()));
        Materia materia = materiaRepository.findById(request.materiaId())
                .orElseThrow(() -> new ResourceNotFoundException("Materia", request.materiaId()));

        currentUserService.validarMaestroDeMateria(materia);

        Calificacion calificacion = calificacionRepository
                .findByAlumnoIdAndMateriaIdAndPeriodo(
                        request.alumnoId(), request.materiaId(), request.periodo())
                .map(existente -> {
                    existente.setCalificacion(request.calificacion());
                    return existente;
                })
                .orElseGet(() -> new Calificacion(
                        alumno, materia, request.calificacion(), request.periodo()));

        return CalificacionResponse.fromEntity(calificacionRepository.save(calificacion));
    }

    @Transactional(readOnly = true)
    public List<CalificacionResponse> listarPorAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno", alumnoId));

        currentUserService.validarAccesoAlumno(alumno);

        return calificacionRepository.findByAlumnoId(alumnoId).stream()
                .map(CalificacionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CalificacionResponse> listarPorMateria(Long materiaId) {
        if (!materiaRepository.existsById(materiaId)) {
            throw new ResourceNotFoundException("Materia", materiaId);
        }

        return calificacionRepository.findByMateriaId(materiaId).stream()
                .map(CalificacionResponse::fromEntity)
                .toList();
    }
}
