package com.bazaarhub.backend.customer.infrastructure.persistence.repository;

import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataAddressRepository extends JpaRepository<AddressEntity, Long> {

    List<AddressEntity> findByUserIdOrderByIsDefaultDescUpdatedAtDesc(Long userId);

    Page<AddressEntity> findByUserId(Long userId, Pageable pageable);

    Optional<AddressEntity> findByIdAndUserId(Long id, Long userId);

    Optional<AddressEntity> findByUserIdAndIsDefaultTrue(Long userId);

    long countByUserId(Long userId);

    Optional<AddressEntity> findFirstByUserIdOrderByUpdatedAtDesc(Long userId);

    @Modifying
    @Query("UPDATE AddressEntity a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefaultAddress(@Param("userId") Long userId);
}
