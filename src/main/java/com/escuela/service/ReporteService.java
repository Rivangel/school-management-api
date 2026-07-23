package com.escuela.service;

import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.entity.Alumno;
import com.escuela.model.entity.Calificacion;
import com.escuela.repository.AlumnoRepository;
import com.escuela.repository.CalificacionRepository;
import com.escuela.security.CurrentUserService;
import com.escuela.util.PdfGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReporteService {

    private final AlumnoRepository alumnoRepository;
    private final CalificacionRepository calificacionRepository;
    private final CurrentUserService currentUserService;
    private final PdfGenerator pdfGenerator;

    public ReporteService(AlumnoRepository alumnoRepository,
                          CalificacionRepository calificacionRepository,
                          CurrentUserService currentUserService,
                          PdfGenerator pdfGenerator) {
        this.alumnoRepository = alumnoRepository;
        this.calificacionRepository = calificacionRepository;
        this.currentUserService = currentUserService;
        this.pdfGenerator = pdfGenerator;
    }

    @Transactional(readOnly = true)
    public BoletaPdf generarBoleta(Long alumnoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno", alumnoId));

        currentUserService.validarAccesoAlumno(alumno);

        List<Calificacion> calificaciones = calificacionRepository.findByAlumnoId(alumnoId);
        byte[] contenido = pdfGenerator.generarBoleta(alumno, calificaciones);
        String nombreArchivo = "boleta-" + alumno.getMatricula() + ".pdf";

        return new BoletaPdf(nombreArchivo, contenido);
    }

    public record BoletaPdf(String nombreArchivo, byte[] contenido) {
    }
}
