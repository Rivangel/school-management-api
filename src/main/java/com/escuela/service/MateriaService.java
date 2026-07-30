package com.escuela.service;

import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.dto.MateriaRequest;
import com.escuela.model.dto.MateriaResponse;
import com.escuela.model.dto.PaginaResponse;
import com.escuela.model.entity.Maestro;
import com.escuela.model.entity.Materia;
import com.escuela.repository.MaestroRepository;
import com.escuela.repository.MateriaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MateriaService {

    private final MateriaRepository materiaRepository;
    private final MaestroRepository maestroRepository;

    public MateriaService(MateriaRepository materiaRepository,
                          MaestroRepository maestroRepository) {
        this.materiaRepository = materiaRepository;
        this.maestroRepository = maestroRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MateriaResponse> listar(Pageable pageable) {
        return PaginaResponse.fromPage(
                materiaRepository.findAll(pageable).map(MateriaResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public MateriaResponse obtenerPorId(Long id) {
        return MateriaResponse.fromEntity(buscarMateria(id));
    }

    @Transactional
    public MateriaResponse crear(MateriaRequest request) {
        Materia materia = new Materia(
                request.nombre(),
                request.creditos(),
                buscarMaestro(request.maestroId()));
        return MateriaResponse.fromEntity(materiaRepository.save(materia));
    }

    @Transactional
    public MateriaResponse actualizar(Long id, MateriaRequest request) {
        Materia materia = buscarMateria(id);
        materia.setNombre(request.nombre());
        materia.setCreditos(request.creditos());
        materia.setMaestro(buscarMaestro(request.maestroId()));
        return MateriaResponse.fromEntity(materiaRepository.save(materia));
    }

    @Transactional
    public void eliminar(Long id) {
        Materia materia = buscarMateria(id);
        materiaRepository.delete(materia);
    }

    private Materia buscarMateria(Long id) {
        return materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia", id));
    }

    private Maestro buscarMaestro(Long id) {
        return maestroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maestro", id));
    }
}
