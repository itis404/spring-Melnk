package ru.studymarket.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.studymarket.domain.Product;
import ru.studymarket.domain.ProductStatus;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"seller", "categories"})
    @Query("""
            select distinct p
            from Product p
            left join p.categories c
            where p.status = :status
              and (:query is null
                or lower(p.title) like lower(concat('%', :query, '%'))
                or lower(p.description) like lower(concat('%', :query, '%'))
                or lower(p.campus) like lower(concat('%', :query, '%')))
              and (:categoryId is null or c.id = :categoryId)
            order by p.createdAt desc
            """)
    List<Product> searchByTextAndCategory(@Param("query") String query,
                                          @Param("categoryId") Long categoryId,
                                          @Param("status") ProductStatus status);

    @EntityGraph(attributePaths = {"seller", "categories", "reviews", "reviews.author"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"seller", "categories"})
    List<Product> findTop8ByStatusOrderByCreatedAtDesc(ProductStatus status);

    @EntityGraph(attributePaths = {"seller", "categories"})
    List<Product> findBySellerUsernameIgnoreCaseAndStatusOrderByCreatedAtDesc(String username, ProductStatus status);

    @EntityGraph(attributePaths = {"seller", "categories"})
    List<Product> findBySellerUsernameIgnoreCaseAndStatusInOrderByUpdatedAtDesc(String username, List<ProductStatus> statuses);

    @EntityGraph(attributePaths = {"seller", "categories"})
    @Query("""
            select distinct p
            from Product p
            where p.status = :status
              and p.id in (
                select r.product.id
                from Review r
                group by r.product.id
                having avg(r.rating) >= :minRating
              )
            order by p.createdAt desc
            """)
    List<Product> findHighlyRatedWithSubselect(@Param("status") ProductStatus status,
                                               @Param("minRating") double minRating);

    @Query("""
            select distinct p
            from Product p
            join p.categories c
            where c.slug = :slug and p.status = :status
            order by p.createdAt desc
            """)
    List<Product> findActiveByCategorySlug(@Param("slug") String slug,
                                           @Param("status") ProductStatus status);
}
