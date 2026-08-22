package com.escuela.controller;

import com.escuela.model.dto.MaestroRequest;
import com.escuela.model.dto.MaestroResponse;
import com.escuela.model.dto.PaginaResponse;
import com.escuela.service.MaestroService;
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
@RequestMapping("/api/maestros")
@Tag(name = "Maestros", description = "Gestión de maestros (ADMIN)")
public class MaestroController {

    private final MaestroService maestroService;

    public MaestroController(MaestroService maestroService) {
        this.maestroService = maestroService;
    }

    @GetMapping
    @Operation(summary = "Listar maestros paginados",
            description = "Acepta los parámetros estándar `page` (base 0), `size` y `sort` "
                    + "(por ejemplo `sort=especialidad,desc`). Por defecto devuelve 20 maestros "
                    + "ordenados por apellido y nombre.")
    public ResponseEntity<PaginaResponse<MaestroResponse>> listar(
            @ParameterObject
            @PageableDefault(size = 20, sort = {"apellido", "nombre"}, direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(maestroService.listar(pageable));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener el maestro del usuario autenticado",
            description = "El token lleva el email y el rol, no el id del registro. Devuelve "
                    + "404 si el usuario autenticado no tiene un maestro vinculado (un ADMIN, "
                    + "por ejemplo). Va antes de `/{id}` porque el router prueba en orden.")
    public ResponseEntity<MaestroResponse> obtenerActual() {
        return ResponseEntity.ok(maestroService.obtenerActual());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un maestro por id")
    public ResponseEntity<MaestroResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(maestroService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Crear un maestro")
    public ResponseEntity<MaestroResponse> crear(@Valid @RequestBody MaestroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maestroService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un maestro")
    public ResponseEntity<MaestroResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody MaestroRequest request) {
        return ResponseEntity.ok(maestroService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un maestro")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        maestroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
