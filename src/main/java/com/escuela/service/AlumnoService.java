package com.escuela.service;

import com.escuela.exception.BadRequestException;
import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.dto.AlumnoRequest;
import com.escuela.model.dto.AlumnoResponse;
import com.escuela.model.dto.PaginaResponse;
import com.escuela.model.entity.Alumno;
import com.escuela.repository.AlumnoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlumnoService {

    private final AlumnoRepository alumnoRepository;

    public AlumnoService(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AlumnoResponse> listar(Pageable pageable) {
        return PaginaResponse.fromPage(
                alumnoRepository.findAll(pageable).map(AlumnoResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public AlumnoResponse obtenerPorId(Long id) {
        return AlumnoResponse.fromEntity(buscarAlumno(id));
    }

    @Transactional
    public AlumnoResponse crear(AlumnoRequest request) {
        if (alumnoRepository.existsByMatricula(request.matricula())) {
            throw new BadRequestException("Ya existe un alumno con la matrícula " + request.matricula());
        }
        if (alumnoRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Ya existe un alumno con el email " + request.email());
        }

        Alumno alumno = new Alumno(
                request.nombre(),
                request.apellido(),
                request.matricula(),
                request.email(),
                request.grupo());
        return AlumnoResponse.fromEntity(alumnoRepository.save(alumno));
    }

    @Transactional
    public AlumnoResponse actualizar(Long id, AlumnoRequest request) {
        Alumno alumno = buscarAlumno(id);

        if (!alumno.getMatricula().equals(request.matricula())
                && alumnoRepository.existsByMatricula(request.matricula())) {
            throw new BadRequestException("Ya existe un alumno con la matrícula " + request.matricula());
        }
        if (!alumno.getEmail().equalsIgnoreCase(request.email())
                && alumnoRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Ya existe un alumno con el email " + request.email());
        }

        alumno.setNombre(request.nombre());
        alumno.setApellido(request.apellido());
        alumno.setMatricula(request.matricula());
        alumno.setEmail(request.email());
        alumno.setGrupo(request.grupo());
        return AlumnoResponse.fromEntity(alumnoRepository.save(alumno));
    }

    @Transactional
    public void eliminar(Long id) {
        Alumno alumno = buscarAlumno(id);
        alumnoRepository.delete(alumno);
    }

    private Alumno buscarAlumno(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno", id));
    }
}
