package com.escuela.controller;

import com.escuela.model.dto.AsistenciaRequest;
import com.escuela.model.dto.AsistenciaResponse;
import com.escuela.service.AsistenciaService;
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
@RequestMapping("/api/asistencia")
@Tag(name = "Asistencia",
        description = "Registro (ADMIN/MAESTRO de la materia) y consulta de asistencia")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @PostMapping
    @Operation(summary = "Registrar asistencia",
            description = "Si ya existe registro para el mismo alumno, materia y fecha, se actualiza")
    public ResponseEntity<AsistenciaResponse> registrar(
            @Valid @RequestBody AsistenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(asistenciaService.registrar(request));
    }

    @GetMapping("/alumno/{id}")
    @Operation(summary = "Consultar asistencia de un alumno",
            description = "Un ALUMNO solo puede consultar su propia asistencia")
    public ResponseEntity<List<AsistenciaResponse>> listarPorAlumno(@PathVariable Long id) {
        return ResponseEntity.ok(asistenciaService.listarPorAlumno(id));
    }
}
