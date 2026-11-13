package com.bazaarhub.backend.identity;

import com.bazaarhub.backend.identity.application.dto.*;
import com.bazaarhub.backend.identity.application.service.AuthService;
import com.bazaarhub.backend.identity.application.service.DeviceService;
import com.bazaarhub.backend.identity.domain.exception.AccountNotActiveException;
import com.bazaarhub.backend.identity.domain.exception.InvalidCredentialsException;
import com.bazaarhub.backend.identity.domain.exception.UserAlreadyExistsException;
import com.bazaarhub.backend.identity.domain.model.AccountStatus;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.PermissionEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.identity.infrastructure.security.JwtService;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private SpringDataRoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private DeviceService deviceService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService, deviceService);
    }

    @Test
    @DisplayName("Successful registration saves user with hashed password and default role")
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe")
                .email("jane@example.com")
                .phone("+919876543210")
                .password("StrongPass123!")
                .confirmPassword("StrongPass123!")
                .build();

        RoleEntity customerRole = RoleEntity.builder().id(1L).name("ROLE_CUSTOMER").build();
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+919876543210")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass123!")).thenReturn("$2a$12$hashedPasswordExample");
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));

        UserEntity savedUser = UserEntity.builder()
                .id(100L)
                .fullName("Jane Doe")
                .email("jane@example.com")
                .phone("+919876543210")
                .passwordHash("$2a$12$hashedPasswordExample")
                .accountStatus(AccountStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .createdAt(Instant.now())
                .build();
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        UserProfileResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Jane Doe", response.getFullName());
        assertEquals("jane@example.com", response.getEmail());
        assertTrue(response.getRoles().contains("ROLE_CUSTOMER"));

        verify(passwordEncoder).encode("StrongPass123!");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Registration fails when password and confirmation do not match")
    void testRegisterPasswordMismatch() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe")
                .email("jane@example.com")
                .password("Password123!")
                .confirmPassword("DifferentPass456!")
                .build();

        BusinessRuleViolationException ex = assertThrows(
                BusinessRuleViolationException.class,
                () -> authService.register(request)
        );
        assertTrue(ex.getMessage().contains("do not match"));
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registration fails when email already exists")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Duplicate User")
                .email("existing@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(request)
        );
        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registration fails when phone already exists")
    void testRegisterDuplicatePhone() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Duplicate Phone")
                .phone("+919876543210")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

        when(userRepository.existsByPhone("+919876543210")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(request)
        );
        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registration fails safely when ROLE_CUSTOMER is missing in database")
    void testRegisterFailsSafelyWhenRoleCustomerMissing() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe")
                .email("jane@example.com")
                .password("StrongPass123!")
                .confirmPassword("StrongPass123!")
                .build();

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass123!")).thenReturn("$2a$12$hashedPasswordExample");
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.empty());
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.empty());

        com.bazaarhub.backend.identity.domain.exception.RoleNotFoundException ex = assertThrows(
                com.bazaarhub.backend.identity.domain.exception.RoleNotFoundException.class,
                () -> authService.register(request)
        );

        assertTrue(ex.getMessage().contains("ROLE_CUSTOMER"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Successful login returns JWT access token and user profile")
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .identifier("jane@example.com")
                .password("StrongPass123!")
                .build();

        PermissionEntity readPerm = PermissionEntity.builder().id(1L).name("PRODUCT_READ").build();
        RoleEntity role = RoleEntity.builder().id(1L).name("ROLE_CUSTOMER").permissions(Set.of(readPerm)).build();
        UserEntity user = UserEntity.builder()
                .id(100L)
                .fullName("Jane Doe")
                .email("jane@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .accountStatus(AccountStatus.ACTIVE)
                .roles(Set.of(role))
                .build();

        when(userRepository.findByIdentifierWithRolesAndPermissions("jane@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass123!", "$2a$12$hashedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(eq(100L), eq("jane@example.com"), any(), eq("Jane Doe"), any(), any(), any()))
                .thenReturn("mock.jwt.token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(86400000L);

        AuthResponse authResponse = authService.login(request);

        assertNotNull(authResponse);
        assertEquals("mock.jwt.token", authResponse.getAccessToken());
        assertEquals("Bearer", authResponse.getTokenType());
        assertEquals(86400000L, authResponse.getExpiresIn());
        assertNotNull(authResponse.getUser());
        assertEquals("Jane Doe", authResponse.getUser().getFullName());

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Login with invalid password throws InvalidCredentialsException")
    void testLoginIncorrectPassword() {
        LoginRequest request = LoginRequest.builder()
                .identifier("jane@example.com")
                .password("WrongPassword")
                .build();

        UserEntity user = UserEntity.builder()
                .id(100L)
                .email("jane@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(userRepository.findByIdentifierWithRolesAndPermissions("jane@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "$2a$12$hashedPassword")).thenReturn(false);

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );
        assertEquals("Invalid email/phone or password", ex.getMessage());
        verify(jwtService, never()).generateAccessToken(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Login with non-existent identifier throws InvalidCredentialsException")
    void testLoginUnknownIdentifier() {
        LoginRequest request = LoginRequest.builder()
                .identifier("unknown@example.com")
                .password("AnyPassword")
                .build();

        when(userRepository.findByIdentifierWithRolesAndPermissions("unknown@example.com"))
                .thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );
        assertEquals("Invalid email/phone or password", ex.getMessage());
    }

    @Test
    @DisplayName("Login with BLOCKED account throws AccountNotActiveException")
    void testLoginBlockedAccount() {
        LoginRequest request = LoginRequest.builder()
                .identifier("blocked@example.com")
                .password("Pass123!")
                .build();

        UserEntity user = UserEntity.builder()
                .id(100L)
                .email("blocked@example.com")
                .passwordHash("$2a$12$hashed")
                .accountStatus(AccountStatus.BLOCKED)
                .build();

        when(userRepository.findByIdentifierWithRolesAndPermissions("blocked@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Pass123!", "$2a$12$hashed")).thenReturn(true);

        AccountNotActiveException ex = assertThrows(
                AccountNotActiveException.class,
                () -> authService.login(request)
        );
        assertTrue(ex.getMessage().contains("blocked"));
        verify(jwtService, never()).generateAccessToken(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Logout deactivates device session")
    void testLogoutDeactivatesDevice() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(50L)
                .email("user@example.com")
                .build();

        String message = authService.logout(principal, "device-123");

        assertNotNull(message);
        assertTrue(message.contains("Logout successful"));
        verify(deviceService).deactivateDevice(50L, "device-123");
    }

    @Test
    @DisplayName("Refresh token throws UnsupportedOperationException documenting schema requirements")
    void testRefreshTokenUnsupported() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("dummy-token").build();

        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> authService.refreshToken(request)
        );
        assertTrue(ex.getMessage().contains("refresh_tokens"));
    }

    @Test
    @DisplayName("Forgot password throws UnsupportedOperationException documenting pending provider")
    void testForgotPasswordUnsupported() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().identifier("user@example.com").build();

        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> authService.forgotPassword(request)
        );
        assertTrue(ex.getMessage().contains("password_reset_tokens"));
    }
}
