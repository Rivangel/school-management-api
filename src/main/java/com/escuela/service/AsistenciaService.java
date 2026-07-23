package com.escuela.service;

import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.dto.AsistenciaRequest;
import com.escuela.model.dto.AsistenciaResponse;
import com.escuela.model.entity.Alumno;
import com.escuela.model.entity.Asistencia;
import com.escuela.model.entity.Materia;
import com.escuela.repository.AlumnoRepository;
import com.escuela.repository.AsistenciaRepository;
import com.escuela.repository.MateriaRepository;
import com.escuela.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final AlumnoRepository alumnoRepository;
    private final MateriaRepository materiaRepository;
    private final CurrentUserService currentUserService;

    public AsistenciaService(AsistenciaRepository asistenciaRepository,
                             AlumnoRepository alumnoRepository,
                             MateriaRepository materiaRepository,
                             CurrentUserService currentUserService) {
        this.asistenciaRepository = asistenciaRepository;
        this.alumnoRepository = alumnoRepository;
        this.materiaRepository = materiaRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Registra la asistencia de un alumno. Si ya existe un registro para el
     * mismo alumno, materia y fecha, se actualiza.
     */
    @Transactional
    public AsistenciaResponse registrar(AsistenciaRequest request) {
        Alumno alumno = alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno", request.alumnoId()));
        Materia materia = materiaRepository.findById(request.materiaId())
                .orElseThrow(() -> new ResourceNotFoundException("Materia", request.materiaId()));

        currentUserService.validarMaestroDeMateria(materia);

        Asistencia asistencia = asistenciaRepository
                .findByAlumnoIdAndMateriaIdAndFecha(
                        request.alumnoId(), request.materiaId(), request.fecha())
                .map(existente -> {
                    existente.setPresente(request.presente());
                    return existente;
                })
                .orElseGet(() -> new Asistencia(
                        alumno, materia, request.fecha(), request.presente()));

        return AsistenciaResponse.fromEntity(asistenciaRepository.save(asistencia));
    }

    @Transactional(readOnly = true)
    public List<AsistenciaResponse> listarPorAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno", alumnoId));

        currentUserService.validarAccesoAlumno(alumno);

        return asistenciaRepository.findByAlumnoId(alumnoId).stream()
                .map(AsistenciaResponse::fromEntity)
                .toList();
    }
}
