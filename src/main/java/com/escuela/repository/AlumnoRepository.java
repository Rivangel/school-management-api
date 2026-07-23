package com.escuela.repository;

import com.escuela.model.entity.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {

    Optional<Alumno> findByEmail(String email);

    boolean existsByMatricula(String matricula);

    boolean existsByEmail(String email);
}
