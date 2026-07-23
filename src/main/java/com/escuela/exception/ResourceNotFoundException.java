package com.escuela.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String recurso, Long id) {
        super(recurso + " con id " + id + " no encontrado");
    }
}
