package com.escuela.security;

import com.escuela.exception.OperacionNoPermitidaException;
import com.escuela.model.entity.Alumno;
import com.escuela.model.entity.Materia;
import com.escuela.model.entity.Rol;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utilidad para consultar la identidad del usuario autenticado y aplicar
 * las reglas de propiedad: un ALUMNO solo accede a sus propios datos y un
 * MAESTRO solo registra información de las materias que imparte.
 */
@Component
public class CurrentUserService {

    public String getEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    public boolean tieneRol(Rol rol) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + rol.name()));
    }

    /**
     * Si el usuario autenticado es ALUMNO, verifica que el alumno consultado
     * sea él mismo (se vinculan por email).
     */
    public void validarAccesoAlumno(Alumno alumno) {
        if (tieneRol(Rol.ALUMNO) && !alumno.getEmail().equalsIgnoreCase(getEmail())) {
            throw new OperacionNoPermitidaException(
                    "Un alumno solo puede consultar su propia información");
        }
    }

    /**
     * Si el usuario autenticado es MAESTRO, verifica que la materia le
     * pertenezca (se vinculan por email).
     */
    public void validarMaestroDeMateria(Materia materia) {
        if (tieneRol(Rol.MAESTRO)
                && !materia.getMaestro().getEmail().equalsIgnoreCase(getEmail())) {
            throw new OperacionNoPermitidaException(
                    "Un maestro solo puede registrar información de sus propias materias");
        }
    }
}
