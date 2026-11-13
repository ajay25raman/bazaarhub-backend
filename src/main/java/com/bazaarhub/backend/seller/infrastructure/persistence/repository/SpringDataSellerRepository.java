package com.bazaarhub.backend.seller.infrastructure.persistence.repository;

import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.SellerVerificationStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataSellerRepository extends JpaRepository<SellerEntity, Long> {

    Optional<SellerEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Page<SellerEntity> findByApprovalStatus(SellerApprovalStatus approvalStatus, Pageable pageable);

    Page<SellerEntity> findByVerificationStatus(SellerVerificationStatus verificationStatus, Pageable pageable);

    Page<SellerEntity> findByApprovalStatusAndVerificationStatus(
            SellerApprovalStatus approvalStatus,
            SellerVerificationStatus verificationStatus,
            Pageable pageable
    );

    @Query("SELECT s FROM SellerEntity s " +
           "LEFT JOIN FETCH s.user u " +
           "LEFT JOIN FETCH s.store st " +
           "WHERE s.id = :id")
    Optional<SellerEntity> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT s FROM SellerEntity s " +
           "LEFT JOIN FETCH s.user u " +
           "LEFT JOIN FETCH s.store st " +
           "WHERE s.user.id = :userId")
    Optional<SellerEntity> findByUserIdWithDetails(@Param("userId") Long userId);
}
