package com.bazaarhub.backend.seller.infrastructure.persistence.repository;

import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataSellerDocumentRepository extends JpaRepository<SellerDocumentEntity, Long> {

    List<SellerDocumentEntity> findBySellerId(Long sellerId);

    Optional<SellerDocumentEntity> findByIdAndSellerId(Long id, Long sellerId);

    boolean existsBySellerIdAndDocumentType(Long sellerId, String documentType);

    long countBySellerId(Long sellerId);
}
