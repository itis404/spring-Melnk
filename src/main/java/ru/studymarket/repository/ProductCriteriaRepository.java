package ru.studymarket.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import ru.studymarket.domain.Category;
import ru.studymarket.domain.Product;
import ru.studymarket.domain.ProductCondition;
import ru.studymarket.domain.ProductStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductCriteriaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Product> findMarketplaceProducts(String query,
                                                 Long categoryId,
                                                 ProductCondition condition,
                                                 BigDecimal minPrice,
                                                 BigDecimal maxPrice,
                                                 String sort) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> product = cq.from(Product.class);
        product.fetch("seller", JoinType.LEFT);
        product.fetch("categories", JoinType.LEFT);
        Join<Product, Category> category = product.join("categories", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(product.get("status"), ProductStatus.ACTIVE));

        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(product.get("title")), pattern),
                    cb.like(cb.lower(product.get("description")), pattern),
                    cb.like(cb.lower(product.get("campus")), pattern)
            ));
        }

        if (categoryId != null) {
            predicates.add(cb.equal(category.get("id"), categoryId));
        }

        if (condition != null) {
            predicates.add(cb.equal(product.get("condition"), condition));
        }

        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(product.get("price"), minPrice));
        }

        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(product.get("price"), maxPrice));
        }

        cq.select(product)
                .distinct(true)
                .where(predicates.toArray(Predicate[]::new));

        switch (sort == null ? "newest" : sort) {
            case "priceAsc" -> cq.orderBy(cb.asc(product.get("price")), cb.desc(product.get("createdAt")));
            case "priceDesc" -> cq.orderBy(cb.desc(product.get("price")), cb.desc(product.get("createdAt")));
            case "titleAsc" -> cq.orderBy(cb.asc(cb.lower(product.get("title"))), cb.desc(product.get("createdAt")));
            default -> cq.orderBy(cb.desc(product.get("createdAt")));
        }

        return entityManager.createQuery(cq).getResultList();
    }
}
