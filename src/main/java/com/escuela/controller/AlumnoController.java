package com.escuela.controller;

import com.escuela.model.dto.AlumnoRequest;
import com.escuela.model.dto.AlumnoResponse;
import com.escuela.model.dto.PaginaResponse;
import com.escuela.service.AlumnoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumnos")
@Tag(name = "Alumnos", description = "Gestión de alumnos (ADMIN; consulta también para MAESTRO)")
public class AlumnoController {

    private final AlumnoService alumnoService;

    public AlumnoController(AlumnoService alumnoService) {
        this.alumnoService = alumnoService;
    }

    @GetMapping
    @Operation(summary = "Listar alumnos paginados",
            description = "Acepta los parámetros estándar `page` (base 0), `size` y `sort` "
                    + "(por ejemplo `sort=apellido,asc`). Por defecto devuelve 20 alumnos "
                    + "ordenados por apellido y nombre.")
    public ResponseEntity<PaginaResponse<AlumnoResponse>> listar(
            @ParameterObject
            @PageableDefault(size = 20, sort = {"apellido", "nombre"}, direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(alumnoService.listar(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un alumno por id")
    public ResponseEntity<AlumnoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(alumnoService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Crear un alumno")
    public ResponseEntity<AlumnoResponse> crear(@Valid @RequestBody AlumnoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alumnoService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un alumno")
    public ResponseEntity<AlumnoResponse> actualizar(@PathVariable Long id,
                                                     @Valid @RequestBody AlumnoRequest request) {
        return ResponseEntity.ok(alumnoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un alumno")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        alumnoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
