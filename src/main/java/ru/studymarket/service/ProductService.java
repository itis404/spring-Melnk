package ru.studymarket.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studymarket.domain.Category;
import ru.studymarket.domain.Product;
import ru.studymarket.domain.ProductCondition;
import ru.studymarket.domain.ProductStatus;
import ru.studymarket.domain.UserAccount;
import ru.studymarket.dto.ProductApiRequest;
import ru.studymarket.exception.ForbiddenOperationException;
import ru.studymarket.exception.ResourceNotFoundException;
import ru.studymarket.form.ProductForm;
import ru.studymarket.repository.CategoryRepository;
import ru.studymarket.repository.ProductCriteriaRepository;
import ru.studymarket.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ProductService {

    public static final String DEFAULT_IMAGE = "/images/studymarket-hero.png";

    private final ProductRepository productRepository;
    private final ProductCriteriaRepository criteriaRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public ProductService(ProductRepository productRepository,
                          ProductCriteriaRepository criteriaRepository,
                          CategoryRepository categoryRepository,
                          UserService userService) {
        this.productRepository = productRepository;
        this.criteriaRepository = criteriaRepository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Product> browse(String query, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        return browse(query, categoryId, null, minPrice, maxPrice, "newest");
    }

    @Transactional(readOnly = true)
    public List<Product> browse(String query,
                                Long categoryId,
                                ProductCondition condition,
                                BigDecimal minPrice,
                                BigDecimal maxPrice,
                                String sort) {
        return criteriaRepository.findMarketplaceProducts(query, categoryId, condition, minPrice, maxPrice, sort);
    }

    @Transactional(readOnly = true)
    public List<Product> featured() {
        List<Product> rated = productRepository.findHighlyRatedWithSubselect(ProductStatus.ACTIVE, 4.5d);
        return rated.isEmpty() ? productRepository.findTop8ByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE) : rated;
    }

    @Transactional(readOnly = true)
    public Product detailed(Long id) {
        return productRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден"));
    }

    @Transactional(readOnly = true)
    public List<Product> activeBySeller(String username) {
        return productRepository.findBySellerUsernameIgnoreCaseAndStatusOrderByCreatedAtDesc(username, ProductStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Product> archivedBySeller(String username) {
        return productRepository.findBySellerUsernameIgnoreCaseAndStatusInOrderByUpdatedAtDesc(
                username,
                List.of(ProductStatus.HIDDEN, ProductStatus.RESERVED, ProductStatus.SOLD)
        );
    }

    @CacheEvict(value = "featuredProducts", allEntries = true)
    public Product create(ProductForm form, String sellerUsername) {
        UserAccount seller = userService.requiredByUsername(sellerUsername);
        Product product = new Product(form.getTitle().trim(), form.getDescription().trim(), form.getPrice(),
                form.getCampus().trim(), seller);
        applyForm(product, form);
        return productRepository.save(product);
    }

    @CacheEvict(value = "featuredProducts", allEntries = true)
    public Product createFromApi(ProductApiRequest request, String sellerUsername) {
        ProductForm form = toForm(request);
        return create(form, sellerUsername);
    }

    @CacheEvict(value = "featuredProducts", allEntries = true)
    public Product update(Long id, ProductForm form, String username) {
        Product product = detailed(id);
        ensureOwner(product, username);
        applyForm(product, form);
        return productRepository.save(product);
    }

    @CacheEvict(value = "featuredProducts", allEntries = true)
    public Product updateFromApi(Long id, ProductApiRequest request, String username) {
        return update(id, toForm(request), username);
    }

    @CacheEvict(value = "featuredProducts", allEntries = true)
    public void hide(Long id, String username) {
        Product product = detailed(id);
        ensureOwner(product, username);
        product.setStatus(ProductStatus.HIDDEN);
    }

    @CacheEvict(value = "featuredProducts", allEntries = true)
    public void delete(Long id, String username) {
        hide(id, username);
    }

    public void markReserved(Product product) {
        product.setStatus(ProductStatus.RESERVED);
        productRepository.save(product);
    }

    @CacheEvict(value = "featuredProducts", allEntries = true)
    public void markActive(Product product) {
        product.setStatus(ProductStatus.ACTIVE);
        productRepository.save(product);
    }

    @CacheEvict(value = "featuredProducts", allEntries = true)
    public void markSold(Product product) {
        product.setStatus(ProductStatus.SOLD);
        productRepository.save(product);
    }

    public ProductForm toForm(Product product) {
        ProductForm form = new ProductForm();
        form.setTitle(product.getTitle());
        form.setDescription(product.getDescription());
        form.setPrice(product.getPrice());
        form.setCampus(product.getCampus());
        form.setImageUrl(product.getImageUrl());
        form.setCondition(product.getCondition());
        form.setCategoryIds(product.getCategories().stream().map(Category::getId).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        return form;
    }

    private void applyForm(Product product, ProductForm form) {
        product.setTitle(form.getTitle().trim());
        product.setDescription(form.getDescription().trim());
        product.setPrice(form.getPrice());
        product.setCampus(form.getCampus().trim());
        product.setImageUrl(normalizeImage(form.getImageUrl()));
        product.setCondition(form.getCondition());
        product.getCategories().clear();
        product.getCategories().addAll(loadCategories(form.getCategoryIds()));
    }

    private Set<Category> loadCategories(Set<Long> categoryIds) {
        List<Category> categories = categoryRepository.findAllByIdIn(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new ResourceNotFoundException("Одна из категорий не найдена");
        }
        return new LinkedHashSet<>(categories);
    }

    private ProductForm toForm(ProductApiRequest request) {
        ProductForm form = new ProductForm();
        form.setTitle(request.title());
        form.setDescription(request.description());
        form.setPrice(request.price());
        form.setCampus(request.campus());
        form.setImageUrl(request.imageUrl());
        form.setCondition(request.condition());
        form.setCategoryIds(request.categoryIds());
        return form;
    }

    private String normalizeImage(String imageUrl) {
        return imageUrl == null || imageUrl.isBlank() ? DEFAULT_IMAGE : imageUrl.trim();
    }

    private void ensureOwner(Product product, String username) {
        if (!product.getSeller().getUsername().equalsIgnoreCase(username)) {
            throw new ForbiddenOperationException("Можно редактировать только свои товары");
        }
    }
}
