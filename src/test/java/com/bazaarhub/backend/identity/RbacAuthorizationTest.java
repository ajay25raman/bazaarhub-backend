package com.bazaarhub.backend.identity;

import com.bazaarhub.backend.identity.domain.model.AccountStatus;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.PermissionEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RbacAuthorizationTest {

    @Test
    @DisplayName("UserPrincipal correctly maps roles with ROLE_ prefix and granular permissions")
    void testUserPrincipalRoleAndPermissionMapping() {
        PermissionEntity perm1 = PermissionEntity.builder().id(1L).name("PRODUCT_READ").build();
        PermissionEntity perm2 = PermissionEntity.builder().id(2L).name("ORDER_CREATE").build();

        RoleEntity roleCustomer = RoleEntity.builder()
                .id(1L)
                .name("CUSTOMER") // Without prefix in DB
                .permissions(Set.of(perm1, perm2))
                .build();

        RoleEntity roleSeller = RoleEntity.builder()
                .id(2L)
                .name("ROLE_SELLER") // With prefix in DB
                .permissions(Set.of())
                .build();

        UserEntity user = UserEntity.builder()
                .id(42L)
                .fullName("Bazaar User")
                .email("user@bazaarhub.com")
                .accountStatus(AccountStatus.ACTIVE)
                .roles(Set.of(roleCustomer, roleSeller))
                .build();

        UserPrincipal principal = UserPrincipal.create(user);

        assertNotNull(principal);
        assertEquals(42L, principal.getId());
        assertEquals("user@bazaarhub.com", principal.getUsername());
        assertTrue(principal.isEnabled());
        assertTrue(principal.isAccountNonLocked());

        // Verify roles
        assertTrue(principal.getRoles().contains("CUSTOMER"));
        assertTrue(principal.getRoles().contains("ROLE_SELLER"));

        // Verify authorities have ROLE_ prefix
        assertTrue(principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        assertTrue(principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SELLER")));

        // Verify granular permissions
        assertTrue(principal.getAuthorities().contains(new SimpleGrantedAuthority("PRODUCT_READ")));
        assertTrue(principal.getAuthorities().contains(new SimpleGrantedAuthority("ORDER_CREATE")));
        assertEquals(4, principal.getAuthorities().size());
    }

    @Test
    @DisplayName("UserPrincipal with BLOCKED status is account locked")
    void testBlockedUserIsLocked() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("blocked@bazaarhub.com")
                .accountStatus(AccountStatus.BLOCKED)
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        assertFalse(principal.isAccountNonLocked());
        assertFalse(principal.isEnabled());
    }

    @Test
    @DisplayName("UserPrincipal with INACTIVE status is not enabled")
    void testInactiveUserIsNotEnabled() {
        UserEntity user = UserEntity.builder()
                .id(2L)
                .email("inactive@bazaarhub.com")
                .accountStatus(AccountStatus.INACTIVE)
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        assertFalse(principal.isEnabled());
        assertTrue(principal.isAccountNonLocked());
    }
}
