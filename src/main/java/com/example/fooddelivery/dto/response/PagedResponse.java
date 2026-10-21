package com.example.fooddelivery.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic response container for paginated resources.
 *
 * @param <T> Payload item type
 */
public record PagedResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean lastPage
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
