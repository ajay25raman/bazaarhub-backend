package com.bazaarhub.backend.seller.infrastructure.persistence.repository;

import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerBankAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataSellerBankAccountRepository extends JpaRepository<SellerBankAccountEntity, Long> {

    List<SellerBankAccountEntity> findBySellerId(Long sellerId);

    Optional<SellerBankAccountEntity> findByIdAndSellerId(Long id, Long sellerId);

    Optional<SellerBankAccountEntity> findBySellerIdAndIsPrimaryTrue(Long sellerId);

    boolean existsBySellerId(Long sellerId);
}
