package com.bazaarhub.backend.review.infrastructure.persistence.entity;

import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.review.domain.model.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA entity mapping strictly to the existing MariaDB `product_reviews` table.
 */
@Entity
@Table(name = "product_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "rating", nullable = false)
    private Byte rating;

    public Integer getRating() {
        return rating != null ? rating.intValue() : null;
    }

    public void setRating(Integer rating) {
        this.rating = rating != null ? rating.byteValue() : null;
    }

    public void setRating(Byte rating) {
        this.rating = rating;
    }

    public static class ProductReviewEntityBuilder {
        public ProductReviewEntityBuilder rating(Integer rating) {
            this.rating = rating != null ? rating.byteValue() : null;
            return this;
        }

        public ProductReviewEntityBuilder rating(Byte rating) {
            this.rating = rating;
            return this;
        }
    }

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "is_verified_purchase", nullable = false)
    @Builder.Default
    private boolean isVerifiedPurchase = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "enum('pending','approved','rejected')")
    @Builder.Default
    private ReviewStatus status = ReviewStatus.approved;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserEntity updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<ReviewImageEntity> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = ReviewStatus.approved;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addImage(ReviewImageEntity image) {
        images.add(image);
        image.setReview(this);
    }

    public void removeImage(ReviewImageEntity image) {
        images.remove(image);
        image.setReview(null);
    }

    public void clearImages() {
        for (ReviewImageEntity image : new ArrayList<>(images)) {
            removeImage(image);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductReviewEntity that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
