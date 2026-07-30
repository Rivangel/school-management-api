package com.escuela.service;

import com.escuela.exception.BadRequestException;
import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.dto.MaestroRequest;
import com.escuela.model.dto.MaestroResponse;
import com.escuela.model.dto.PaginaResponse;
import com.escuela.model.entity.Maestro;
import com.escuela.repository.MaestroRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaestroService {

    private final MaestroRepository maestroRepository;

    public MaestroService(MaestroRepository maestroRepository) {
        this.maestroRepository = maestroRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MaestroResponse> listar(Pageable pageable) {
        return PaginaResponse.fromPage(
                maestroRepository.findAll(pageable).map(MaestroResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public MaestroResponse obtenerPorId(Long id) {
        return MaestroResponse.fromEntity(buscarMaestro(id));
    }

    @Transactional
    public MaestroResponse crear(MaestroRequest request) {
        if (maestroRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Ya existe un maestro con el email " + request.email());
        }

        Maestro maestro = new Maestro(
                request.nombre(),
                request.apellido(),
                request.email(),
                request.especialidad());
        return MaestroResponse.fromEntity(maestroRepository.save(maestro));
    }

    @Transactional
    public MaestroResponse actualizar(Long id, MaestroRequest request) {
        Maestro maestro = buscarMaestro(id);

        if (!maestro.getEmail().equalsIgnoreCase(request.email())
                && maestroRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Ya existe un maestro con el email " + request.email());
        }

        maestro.setNombre(request.nombre());
        maestro.setApellido(request.apellido());
        maestro.setEmail(request.email());
        maestro.setEspecialidad(request.especialidad());
        return MaestroResponse.fromEntity(maestroRepository.save(maestro));
    }

    @Transactional
    public void eliminar(Long id) {
        Maestro maestro = buscarMaestro(id);
        maestroRepository.delete(maestro);
    }

    private Maestro buscarMaestro(Long id) {
        return maestroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maestro", id));
    }
}
