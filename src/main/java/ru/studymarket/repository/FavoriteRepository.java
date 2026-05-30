package ru.studymarket.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.studymarket.domain.Favorite;
import ru.studymarket.domain.Product;
import ru.studymarket.domain.UserAccount;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserAndProduct(UserAccount user, Product product);

    long countByProductId(Long productId);

    @EntityGraph(attributePaths = {"product", "product.seller", "product.categories"})
    List<Favorite> findByUserUsernameIgnoreCaseOrderByCreatedAtDesc(String username);

    @Query("select f.product.id from Favorite f where lower(f.user.username) = lower(:username)")
    Set<Long> findProductIdsByUsername(@Param("username") String username);
}
