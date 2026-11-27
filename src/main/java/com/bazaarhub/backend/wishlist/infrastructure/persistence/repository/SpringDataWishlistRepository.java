package com.bazaarhub.backend.wishlist.infrastructure.persistence.repository;

import com.bazaarhub.backend.wishlist.infrastructure.persistence.entity.WishlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataWishlistRepository extends JpaRepository<WishlistEntity, Long> {

    Optional<WishlistEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
