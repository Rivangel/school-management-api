package com.escuela.repository;

import com.escuela.model.entity.Materia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MateriaRepository extends JpaRepository<Materia, Long> {

    /**
     * Materias de un maestro, paginadas.
     *
     * <p>Devuelve {@link Page} y no {@code List} porque quien la llama es el listado, que
     * pagina en el servidor: con una lista, filtrar por maestro desharía la paginación y
     * el cliente recibiría de golpe todas las materias del maestro.
     */
    Page<Materia> findByMaestroId(Long maestroId, Pageable pageable);
}
