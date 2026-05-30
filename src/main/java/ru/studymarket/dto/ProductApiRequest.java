package ru.studymarket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.studymarket.domain.ProductCondition;

import java.math.BigDecimal;
import java.util.Set;

public record ProductApiRequest(
        @NotBlank @Size(max = 140) String title,
        @NotBlank @Size(min = 20, max = 2500) String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotBlank @Size(max = 160) String campus,
        @Size(max = 700) String imageUrl,
        @NotNull ProductCondition condition,
        @NotEmpty Set<Long> categoryIds
) {
}
