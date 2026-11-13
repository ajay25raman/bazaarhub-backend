package com.bazaarhub.backend.seller.infrastructure.persistence.repository;

import com.bazaarhub.backend.seller.infrastructure.persistence.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataStoreRepository extends JpaRepository<StoreEntity, Long> {

    Optional<StoreEntity> findBySellerId(Long sellerId);

    boolean existsBySellerId(Long sellerId);

    Optional<StoreEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
