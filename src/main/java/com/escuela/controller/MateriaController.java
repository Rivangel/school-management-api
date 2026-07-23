package com.escuela.controller;

import com.escuela.model.dto.MateriaRequest;
import com.escuela.model.dto.MateriaResponse;
import com.escuela.service.MateriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/materias")
@Tag(name = "Materias", description = "Gestión de materias (ADMIN; consulta para todos los roles)")
public class MateriaController {

    private final MateriaService materiaService;

    public MateriaController(MateriaService materiaService) {
        this.materiaService = materiaService;
    }

    @GetMapping
    @Operation(summary = "Listar todas las materias")
    public ResponseEntity<List<MateriaResponse>> listar() {
        return ResponseEntity.ok(materiaService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una materia por id")
    public ResponseEntity<MateriaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(materiaService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Crear una materia")
    public ResponseEntity<MateriaResponse> crear(@Valid @RequestBody MateriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materiaService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una materia")
    public ResponseEntity<MateriaResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody MateriaRequest request) {
        return ResponseEntity.ok(materiaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una materia")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        materiaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
