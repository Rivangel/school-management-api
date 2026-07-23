package com.escuela.exception;

/**
 * Se lanza cuando un usuario autenticado intenta acceder a un recurso
 * que no le pertenece (p. ej. un alumno consultando calificaciones ajenas
 * o un maestro calificando una materia que no imparte).
 */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String message) {
        super(message);
    }
}
