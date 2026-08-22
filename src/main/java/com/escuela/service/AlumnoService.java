package com.escuela.service;

import com.escuela.exception.BadRequestException;
import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.dto.AlumnoRequest;
import com.escuela.model.dto.AlumnoResponse;
import com.escuela.model.dto.PaginaResponse;
import com.escuela.model.entity.Alumno;
import com.escuela.repository.AlumnoRepository;
import com.escuela.security.CurrentUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlumnoService {

    private final AlumnoRepository alumnoRepository;
    private final CurrentUserService currentUserService;

    public AlumnoService(AlumnoRepository alumnoRepository,
                         CurrentUserService currentUserService) {
        this.alumnoRepository = alumnoRepository;
        this.currentUserService = currentUserService;
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

    /**
     * El alumno vinculado al usuario autenticado, resuelto por email.
     *
     * <p>Es la pieza que le faltaba al ALUMNO para consultar lo suyo:
     * {@code /api/calificaciones/alumno/{id}} pide un id y el token no lo lleva, y el
     * listado de alumnos —donde podría buscarse— está cerrado para él precisamente
     * porque no debe ver a los demás. Sin esto, el único rol que sólo puede consultar sus
     * propios datos es el que no puede nombrarlos.
     *
     * <p>No abre ninguna puerta nueva: devuelve **su** registro, el mismo que
     * {@code validarAccesoAlumno} ya le permitiría leer. Un ADMIN o un MAESTRO no tienen
     * alumno vinculado y reciben un 404 con esa explicación.
     */
    @Transactional(readOnly = true)
    public AlumnoResponse obtenerActual() {
        String email = currentUserService.getEmail();
        return alumnoRepository.findByEmail(email)
                .map(AlumnoResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay ningún alumno vinculado a " + email));
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
