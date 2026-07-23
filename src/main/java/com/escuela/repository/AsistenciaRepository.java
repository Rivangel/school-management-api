package com.escuela.repository;

import com.escuela.model.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    List<Asistencia> findByAlumnoId(Long alumnoId);

    Optional<Asistencia> findByAlumnoIdAndMateriaIdAndFecha(Long alumnoId, Long materiaId, LocalDate fecha);
}
