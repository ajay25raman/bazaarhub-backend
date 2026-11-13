package com.bazaarhub.backend.identity.infrastructure.persistence.repository;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByPhone(String phone);

    @Query("SELECT u FROM UserEntity u WHERE u.email = :identifier OR u.phone = :identifier")
    Optional<UserEntity> findByIdentifier(@Param("identifier") String identifier);

    @Query("SELECT DISTINCT u FROM UserEntity u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.email = :identifier OR u.phone = :identifier")
    Optional<UserEntity> findByIdentifierWithRolesAndPermissions(@Param("identifier") String identifier);

    @Query("SELECT DISTINCT u FROM UserEntity u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.id = :id")
    Optional<UserEntity> findByIdWithRolesAndPermissions(@Param("id") Long id);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
