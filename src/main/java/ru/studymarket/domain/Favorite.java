package ru.studymarket.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "favorites", uniqueConstraints = {
        @UniqueConstraint(name = "uk_favorite_user_product", columnNames = {"user_id", "product_id"})
})
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Favorite() {
    }

    public Favorite(UserAccount user, Product product) {
        this.user = user;
        this.product = product;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public Product getProduct() {
        return product;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
