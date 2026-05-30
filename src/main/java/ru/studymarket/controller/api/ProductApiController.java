package ru.studymarket.controller.api;

import jakarta.validation.Valid;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.studymarket.domain.Product;
import ru.studymarket.dto.ProductApiRequest;
import ru.studymarket.dto.ProductDto;
import ru.studymarket.service.ProductService;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;
    private final ConversionService conversionService;

    public ProductApiController(ProductService productService, ConversionService conversionService) {
        this.productService = productService;
        this.conversionService = conversionService;
    }

    @GetMapping
    public List<ProductDto> list(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) Long categoryId,
                                 @RequestParam(required = false) BigDecimal minPrice,
                                 @RequestParam(required = false) BigDecimal maxPrice) {
        return productService.browse(q, categoryId, minPrice, maxPrice).stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable Long id) {
        return toDto(productService.detailed(id));
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@Valid @RequestBody ProductApiRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDto(productService.createFromApi(request, principal.getName())));
    }

    @PutMapping("/{id}")
    public ProductDto update(@PathVariable Long id,
                             @Valid @RequestBody ProductApiRequest request,
                             Principal principal) {
        return toDto(productService.updateFromApi(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        productService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    private ProductDto toDto(Product product) {
        return conversionService.convert(product, ProductDto.class);
    }
}
