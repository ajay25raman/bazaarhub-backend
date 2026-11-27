package com.bazaarhub.backend.wishlist.infrastructure.persistence.repository;

import com.bazaarhub.backend.wishlist.infrastructure.persistence.entity.WishlistItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataWishlistItemRepository extends JpaRepository<WishlistItemEntity, Long> {

    Page<WishlistItemEntity> findByWishlistIdOrderByCreatedAtDesc(Long wishlistId, Pageable pageable);

    Optional<WishlistItemEntity> findByWishlistIdAndProductId(Long wishlistId, Long productId);

    boolean existsByWishlistIdAndProductId(Long wishlistId, Long productId);

    int countByWishlistId(Long wishlistId);

    @Modifying
    @Query("DELETE FROM WishlistItemEntity w WHERE w.wishlist.id = :wishlistId")
    void deleteAllByWishlistId(@Param("wishlistId") Long wishlistId);

    @Modifying
    @Query("DELETE FROM WishlistItemEntity w WHERE w.wishlist.id = :wishlistId AND w.product.id = :productId")
    void deleteByWishlistIdAndProductId(@Param("wishlistId") Long wishlistId, @Param("productId") Long productId);
}
