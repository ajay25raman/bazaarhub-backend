package com.bazaarhub.backend.cart.infrastructure.persistence.repository;

import com.bazaarhub.backend.cart.domain.model.CartStatus;
import com.bazaarhub.backend.cart.infrastructure.persistence.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataCartRepository extends JpaRepository<CartEntity, Long> {

    Optional<CartEntity> findByUserId(Long userId);

    Optional<CartEntity> findByUserIdAndStatus(Long userId, CartStatus status);

    boolean existsByUserId(Long userId);

    @Query("SELECT c FROM CartEntity c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.variant v LEFT JOIN FETCH v.product p WHERE c.user.id = :userId AND c.status = :status")
    Optional<CartEntity> findByUserIdAndStatusWithItems(@Param("userId") Long userId, @Param("status") CartStatus status);
}
