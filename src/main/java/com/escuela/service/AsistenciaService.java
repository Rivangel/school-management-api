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

import java.time.LocalDate;
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

    /**
     * La asistencia ya registrada de una materia en un día concreto.
     *
     * <p>Es lo que le faltaba a quien pasa lista: sin esto, la única forma de saber si
     * un grupo ya tenía asistencia ese día era preguntar alumno por alumno, y una
     * pantalla que no lo sabe sólo puede volver a mandarlo todo cada vez.
     *
     * <p>Leerla no exige ser el maestro de la materia, igual que en calificaciones;
     * registrarla sí, y eso lo sigue comprobando {@link #registrar}.
     */
    @Transactional(readOnly = true)
    public List<AsistenciaResponse> listarPorMateriaYFecha(Long materiaId, LocalDate fecha) {
        if (!materiaRepository.existsById(materiaId)) {
            throw new ResourceNotFoundException("Materia", materiaId);
        }

        return asistenciaRepository.findByMateriaIdAndFecha(materiaId, fecha).stream()
                .map(AsistenciaResponse::fromEntity)
                .toList();
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
