package ru.studymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.studymarket.domain.MarketplaceOrder;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<MarketplaceOrder, Long> {

    @Query("""
            select distinct o
            from MarketplaceOrder o
            left join fetch o.items i
            left join fetch i.product p
            left join fetch o.buyer
            left join fetch o.seller
            where lower(o.buyer.username) = lower(:username)
               or lower(o.seller.username) = lower(:username)
            order by o.createdAt desc
            """)
    List<MarketplaceOrder> findVisibleForUser(@Param("username") String username);

    @Query("""
            select distinct o
            from MarketplaceOrder o
            left join fetch o.items i
            left join fetch i.product p
            left join fetch o.buyer
            left join fetch o.seller
            where o.id = :id
              and (lower(o.buyer.username) = lower(:username)
                   or lower(o.seller.username) = lower(:username))
            """)
    Optional<MarketplaceOrder> findVisibleById(@Param("id") Long id, @Param("username") String username);
}
