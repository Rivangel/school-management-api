package com.escuela.repository;

import com.escuela.model.entity.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

    List<Calificacion> findByAlumnoId(Long alumnoId);

    List<Calificacion> findByMateriaId(Long materiaId);

    Optional<Calificacion> findByAlumnoIdAndMateriaIdAndPeriodo(Long alumnoId, Long materiaId, String periodo);
}
