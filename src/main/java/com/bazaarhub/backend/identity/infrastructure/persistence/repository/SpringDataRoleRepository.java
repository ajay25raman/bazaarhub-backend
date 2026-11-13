package com.bazaarhub.backend.identity.infrastructure.persistence.repository;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataRoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);

    @Query("SELECT DISTINCT r FROM RoleEntity r LEFT JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<RoleEntity> findByNameWithPermissions(@Param("name") String name);

    boolean existsByName(String name);
}
