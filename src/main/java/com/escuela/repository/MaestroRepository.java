package com.escuela.repository;

import com.escuela.model.entity.Maestro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaestroRepository extends JpaRepository<Maestro, Long> {

    Optional<Maestro> findByEmail(String email);

    boolean existsByEmail(String email);
}
