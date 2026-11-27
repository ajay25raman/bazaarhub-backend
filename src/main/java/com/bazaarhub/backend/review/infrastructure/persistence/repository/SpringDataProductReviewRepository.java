package com.bazaarhub.backend.review.infrastructure.persistence.repository;

import com.bazaarhub.backend.review.domain.model.ReviewStatus;
import com.bazaarhub.backend.review.infrastructure.persistence.entity.ProductReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataProductReviewRepository extends JpaRepository<ProductReviewEntity, Long> {

    Optional<ProductReviewEntity> findByProductIdAndUserId(Long productId, Long userId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    Page<ProductReviewEntity> findByUserId(Long userId, Pageable pageable);

    Optional<ProductReviewEntity> findByIdAndUserId(Long id, Long userId);

    Page<ProductReviewEntity> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    Page<ProductReviewEntity> findByProductIdAndStatusAndRating(Long productId, ReviewStatus status, Integer rating, Pageable pageable);

    @Query("""
        SELECT 
            COUNT(r.id),
            COALESCE(AVG(r.rating), 0.0),
            COALESCE(SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END), 0)
        FROM ProductReviewEntity r
        WHERE r.product.id = :productId
          AND r.status = com.bazaarhub.backend.review.domain.model.ReviewStatus.approved
    """)
    List<Object[]> getRatingAggregateByProductId(@Param("productId") Long productId);

    @Query("""
        SELECT r FROM ProductReviewEntity r
        WHERE r.product.seller.user.id = :sellerUserId
    """)
    Page<ProductReviewEntity> findBySellerUserId(@Param("sellerUserId") Long sellerUserId, Pageable pageable);

    @Query("""
        SELECT r FROM ProductReviewEntity r
        WHERE r.product.seller.user.id = :sellerUserId 
          AND r.product.id = :productId
    """)
    Page<ProductReviewEntity> findBySellerUserIdAndProductId(
            @Param("sellerUserId") Long sellerUserId,
            @Param("productId") Long productId,
            Pageable pageable
    );

    Page<ProductReviewEntity> findByStatus(ReviewStatus status, Pageable pageable);
}
