package ru.studymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.studymarket.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.seller.id = :sellerId")
    double averageRatingForSeller(@Param("sellerId") Long sellerId);
}
