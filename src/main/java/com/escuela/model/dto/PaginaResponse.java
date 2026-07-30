package com.escuela.model.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envoltorio de paginación con un contrato JSON estable.
 *
 * <p>Se prefiere sobre serializar directamente {@link Page}: la estructura que
 * produce {@code PageImpl} depende de detalles internos de Spring Data (y desde
 * Spring Boot 3.3 su serialización directa está desaconsejada), mientras que
 * aquí los campos son explícitos y el frontend puede reflejarlos tal cual.
 *
 * @param content       elementos de la página actual
 * @param page          índice de la página, empezando en 0
 * @param size          tamaño solicitado de página
 * @param totalElements total de elementos en todas las páginas
 * @param totalPages    número total de páginas
 * @param first         indica si es la primera página
 * @param last          indica si es la última página
 */
public record PaginaResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PaginaResponse<T> fromPage(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
