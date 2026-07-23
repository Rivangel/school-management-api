package com.escuela.controller;

import com.escuela.model.dto.CalificacionRequest;
import com.escuela.model.dto.CalificacionResponse;
import com.escuela.service.CalificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/calificaciones")
@Tag(name = "Calificaciones",
        description = "Registro (ADMIN/MAESTRO de la materia) y consulta de calificaciones")
public class CalificacionController {

    private final CalificacionService calificacionService;

    public CalificacionController(CalificacionService calificacionService) {
        this.calificacionService = calificacionService;
    }

    @PostMapping
    @Operation(summary = "Registrar una calificación",
            description = "Si ya existe para el mismo alumno, materia y periodo, se actualiza")
    public ResponseEntity<CalificacionResponse> registrar(
            @Valid @RequestBody CalificacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(calificacionService.registrar(request));
    }

    @GetMapping("/alumno/{id}")
    @Operation(summary = "Consultar calificaciones de un alumno",
            description = "Un ALUMNO solo puede consultar sus propias calificaciones")
    public ResponseEntity<List<CalificacionResponse>> listarPorAlumno(@PathVariable Long id) {
        return ResponseEntity.ok(calificacionService.listarPorAlumno(id));
    }

    @GetMapping("/materia/{id}")
    @Operation(summary = "Consultar calificaciones de una materia")
    public ResponseEntity<List<CalificacionResponse>> listarPorMateria(@PathVariable Long id) {
        return ResponseEntity.ok(calificacionService.listarPorMateria(id));
    }
}
