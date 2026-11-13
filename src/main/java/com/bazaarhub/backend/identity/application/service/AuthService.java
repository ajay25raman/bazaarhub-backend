package com.bazaarhub.backend.identity.application.service;

import com.bazaarhub.backend.identity.application.dto.*;
import com.bazaarhub.backend.identity.domain.exception.AccountNotActiveException;
import com.bazaarhub.backend.identity.domain.exception.InvalidCredentialsException;
import com.bazaarhub.backend.identity.domain.exception.RoleNotFoundException;
import com.bazaarhub.backend.identity.domain.exception.UserAlreadyExistsException;
import com.bazaarhub.backend.identity.domain.model.AccountStatus;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.identity.infrastructure.security.JwtService;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;
import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enterprise Application Service coordinating authentication, registration,
 * JWT issuance, RBAC evaluation, and session lifecycle.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SpringDataUserRepository userRepository;
    private final SpringDataRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DeviceService deviceService;

    public AuthService(SpringDataUserRepository userRepository,
                       SpringDataRoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       DeviceService deviceService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.deviceService = deviceService;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        if ((request.getEmail() == null || request.getEmail().isBlank()) &&
            (request.getPhone() == null || request.getPhone().isBlank())) {
            throw new BusinessRuleViolationException("Either an email address or mobile phone number must be provided");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessRuleViolationException("Password and confirm password do not match");
        }

        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null;
        String normalizedPhone = request.getPhone() != null ? request.getPhone().trim() : null;

        if (normalizedEmail != null && userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("An account with this email address already exists");
        }

        if (normalizedPhone != null && userRepository.existsByPhone(normalizedPhone)) {
            throw new UserAlreadyExistsException("An account with this phone number already exists");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .or(() -> roleRepository.findByName("CUSTOMER"))
                .orElseThrow(() -> {
                    log.error("[REGISTRATION FAILED] Default customer role (ROLE_CUSTOMER) not found in database. User registration aborted.");
                    return new RoleNotFoundException("ROLE_CUSTOMER");
                });

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(customerRole);

        UserEntity user = UserEntity.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .phone(normalizedPhone)
                .passwordHash(passwordHash)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .roles(roles)
                .build();

        UserEntity savedUser = userRepository.save(user);
        log.info("[REGISTRATION] Successfully registered new user ID: {}, email: '{}'", savedUser.getId(), savedUser.getEmail());

        return mapToProfileResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        String normalizedIdentifier = identifier.contains("@") ? identifier.toLowerCase() : identifier;

        UserEntity user = userRepository.findByIdentifierWithRolesAndPermissions(normalizedIdentifier)
                .orElseThrow(() -> {
                    log.warn("[LOGIN FAILED] Account not found for identifier: '{}'", maskIdentifier(identifier));
                    return new InvalidCredentialsException("Invalid email/phone or password");
                });

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("[LOGIN FAILED] Incorrect password for user ID: {}", user.getId());
            throw new InvalidCredentialsException("Invalid email/phone or password");
        }

        if (user.getAccountStatus() == AccountStatus.BLOCKED) {
            log.warn("[LOGIN REJECTED] User ID: {} is BLOCKED", user.getId());
            throw new AccountNotActiveException("Your account has been blocked. Please contact customer support.");
        }

        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            log.warn("[LOGIN REJECTED] User ID: {} is INACTIVE", user.getId());
            throw new AccountNotActiveException("Your account is currently inactive.");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String deviceId = null;
        if (request.getDeviceInfo() != null) {
            deviceService.registerOrUpdateDevice(user, request.getDeviceInfo());
            deviceId = request.getDeviceInfo().getDeviceId();
        }

        Set<String> roleNames = extractRoleNames(user);
        Set<String> permissionNames = extractPermissionNames(user);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                deviceId,
                roleNames,
                permissionNames
        );

        log.info("[LOGIN SUCCESS] User ID: {} authenticated successfully. Roles: {}", user.getId(), roleNames);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(mapToProfileResponse(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(UserPrincipal principal) {
        UserEntity user = userRepository.findByIdWithRolesAndPermissions(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        return mapToProfileResponse(user);
    }

    @Transactional
    public String logout(UserPrincipal principal, String deviceId) {
        if (principal != null) {
            deviceService.deactivateDevice(principal.getId(), deviceId);
            log.info("[LOGOUT] User ID: {} logged out. Device ID '{}' deactivated.", principal.getId(), deviceId);
        }
        return "Logout successful. Associated device session deactivated. " +
               "Stateless JWT access tokens remain valid until expiration; client must discard token.";
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        throw new UnsupportedOperationException(
                "Refresh token persistence is not supported by the current database schema. " +
                "Dedicated 'refresh_tokens' table migration (storing token hash, expiry, revocation) is required in Phase 2."
        );
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        throw new UnsupportedOperationException(
                "Password recovery is pending external Email/SMS notification provider configuration " +
                "and 'password_reset_tokens' schema migration in Phase 2."
        );
    }

    public String resetPassword(ResetPasswordRequest request) {
        throw new UnsupportedOperationException(
                "Password reset is pending external Email/SMS notification provider configuration " +
                "and 'password_reset_tokens' schema migration in Phase 2."
        );
    }

    private UserProfileResponse mapToProfileResponse(UserEntity user) {
        Set<String> roles = extractRoleNames(user);
        Set<String> permissions = extractPermissionNames(user);

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .accountStatus(user.getAccountStatus())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .roles(roles)
                .permissions(permissions)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private Set<String> extractRoleNames(UserEntity user) {
        if (user.getRoles() == null) return Set.of();
        return user.getRoles().stream()
                .map(role -> {
                    String name = role.getName();
                    return name.startsWith("ROLE_") ? name : "ROLE_" + name;
                })
                .collect(Collectors.toSet());
    }

    private Set<String> extractPermissionNames(UserEntity user) {
        if (user.getRoles() == null) return Set.of();
        Set<String> permissions = new HashSet<>();
        user.getRoles().forEach(role -> {
            if (role.getPermissions() != null) {
                role.getPermissions().forEach(permission -> permissions.add(permission.getName()));
            }
        });
        return permissions;
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null) return "***";
        if (identifier.contains("@")) {
            int atIndex = identifier.indexOf("@");
            return identifier.charAt(0) + "***" + identifier.substring(atIndex);
        }
        if (identifier.length() > 4) {
            return "***" + identifier.substring(identifier.length() - 4);
        }
        return "***";
    }
}
