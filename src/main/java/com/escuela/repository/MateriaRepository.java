package com.escuela.repository;

import com.escuela.model.entity.Materia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MateriaRepository extends JpaRepository<Materia, Long> {

    List<Materia> findByMaestroId(Long maestroId);
}
