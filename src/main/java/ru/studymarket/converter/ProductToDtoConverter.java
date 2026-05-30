package ru.studymarket.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.studymarket.domain.Category;
import ru.studymarket.domain.Product;
import ru.studymarket.dto.ProductDto;

@Component
public class ProductToDtoConverter implements Converter<Product, ProductDto> {

    @Override
    public ProductDto convert(Product product) {
        return new ProductDto(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getCampus(),
                product.getImageUrl(),
                product.getCondition().getLabel(),
                product.getStatus().getLabel(),
                product.getSeller().getFullName(),
                product.getCategories().stream().map(Category::getName).toList()
        );
    }
}
