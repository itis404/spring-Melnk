package ru.studymarket.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studymarket.dto.CategoryOption;
import ru.studymarket.repository.CategoryRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Cacheable("categories")
    public List<CategoryOption> findAll() {
        return categoryRepository.findAll().stream()
                .map(category -> new CategoryOption(category.getId(), category.getName(), category.getSlug(), category.getAccentColor()))
                .toList();
    }
}
