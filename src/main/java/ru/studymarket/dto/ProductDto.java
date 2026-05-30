package ru.studymarket.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDto(
        Long id,
        String title,
        String description,
        BigDecimal price,
        String campus,
        String imageUrl,
        String condition,
        String status,
        String seller,
        List<String> categories
) {
}
