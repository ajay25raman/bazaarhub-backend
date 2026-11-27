package com.bazaarhub.backend.bootstrap.config;

import com.bazaarhub.backend.identity.infrastructure.security.CustomAccessDeniedHandler;
import com.bazaarhub.backend.identity.infrastructure.security.JwtAuthenticationEntryPoint;
import com.bazaarhub.backend.identity.infrastructure.security.JwtAuthenticationFilter;
import com.bazaarhub.backend.shared.infrastructure.ratelimit.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Enterprise Spring Security configuration for BazaarHub Backend.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Stateless JWT Bearer token authentication via {@link JwtAuthenticationFilter}.</li>
 *   <li>Database-driven RBAC and method-level security with {@link EnableMethodSecurity}.</li>
 *   <li>BCrypt password hashing with default strength 12.</li>
 *   <li>Standardized error handling via {@link JwtAuthenticationEntryPoint} and {@link CustomAccessDeniedHandler}.</li>
 *   <li>Strict CORS policy for authorized frontend origins.</li>
 *   <li>Disabled CSRF protection tailored for stateless REST APIs.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Value("${bazaarhub.cors.allowed-origins:*}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitingFilter rateLimitingFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF for stateless REST architecture
            .csrf(AbstractHttpConfigurer::disable)

            // 2. Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 3. Exception Handling (Standardized 401 & 403 ApiResponse payloads)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // 4. Stateless Session Management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 5. URL Authorization Rules
            .authorizeHttpRequests(authorize -> authorize
                // Actuator Health monitoring
                .requestMatchers("/actuator/health").permitAll()

                // Swagger / OpenAPI 3 Documentation Endpoints
                .requestMatchers(
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/index.html",
                    "/swagger-ui/**",
                    "/swagger-resources",
                    "/swagger-resources/**"
                ).permitAll()

                // Public Authentication Endpoints
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/forgot-password",
                    "/api/v1/auth/reset-password"
                ).permitAll()

                // Explicit Protected Authentication Endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()

                // Public Catalog, Brand, Product, Search & Discovery GET Endpoints (Read-only)
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/categories",
                    "/api/v1/categories/**",
                    "/api/v1/brands",
                    "/api/v1/brands/**",
                    "/api/v1/products",
                    "/api/v1/products/**",
                    "/api/v1/search",
                    "/api/v1/search/**",
                    "/api/v1/discovery",
                    "/api/v1/discovery/**"
                ).permitAll()

                // Public Payment Webhook Endpoints
                .requestMatchers("/api/v1/payments/webhooks/**").permitAll()

                // Public AI Endpoints (Recommendations, Review Summary, Guest Chat)
                .requestMatchers("/api/v1/public/ai/**").permitAll()

                // Public Analytics Event Tracking
                .requestMatchers("/api/v1/public/analytics/**").permitAll()

                // Public Shipping & Tracking Endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/shipments/track/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/shipments/rate-quote").permitAll()

                // Customer Payment endpoints
                .requestMatchers("/api/v1/payments/**").hasAnyAuthority(
                    "PAYMENT_CREATE", "PAYMENT_READ",
                    "ROLE_CUSTOMER", "ROLE_ADMIN"
                )

                // Customer endpoints (Profile, Address, Wishlist, Reviews)
                .requestMatchers("/api/v1/customer/**").hasAnyAuthority(
                    "CUSTOMER_PROFILE_READ", "CUSTOMER_PROFILE_UPDATE",
                    "WISHLIST_READ", "WISHLIST_UPDATE",
                    "REVIEW_CREATE", "REVIEW_READ", "REVIEW_UPDATE", "REVIEW_DELETE",
                    "ROLE_CUSTOMER", "ROLE_ADMIN"
                )

                // Cart & Checkout endpoints
                .requestMatchers("/api/v1/cart/**", "/api/v1/checkout/**").hasAnyAuthority(
                    "CART_READ", "CART_UPDATE",
                    "ROLE_CUSTOMER", "ROLE_ADMIN"
                )

                // Customer Order endpoints
                .requestMatchers("/api/v1/orders/**").hasAnyAuthority(
                    "ORDER_CREATE", "ORDER_READ", "ORDER_CANCEL",
                    "ROLE_CUSTOMER", "ROLE_ADMIN"
                )

                // Seller Order endpoints
                .requestMatchers("/api/v1/seller/orders/**").hasAnyAuthority(
                    "SELLER_ORDER_MANAGE",
                    "ROLE_SELLER", "ROLE_ADMIN"
                )

                // Admin Order endpoints
                .requestMatchers("/api/v1/admin/orders/**").hasAnyAuthority(
                    "ADMIN_ORDER_MANAGE", "ROLE_ADMIN"
                )

                // Inventory endpoints
                .requestMatchers("/api/v1/admin/inventory/**").hasAnyAuthority("INVENTORY_AUDIT", "INVENTORY_READ", "ROLE_ADMIN")
                .requestMatchers("/api/v1/seller/inventory/**").hasAnyAuthority("INVENTORY_UPDATE", "INVENTORY_READ", "ROLE_SELLER")
                .requestMatchers("/api/v1/internal/inventory/**").hasAnyAuthority("INVENTORY_AUDIT", "ROLE_ADMIN")

                // Delivery partner onboarding (any authenticated user)
                .requestMatchers(HttpMethod.POST, "/api/v1/delivery/onboard").authenticated()

                // Delivery tracking (Customer, Delivery Partner, Admin)
                .requestMatchers("/api/v1/delivery/tracking/**").hasAnyAuthority(
                    "ORDER_READ", "ROLE_CUSTOMER", "ROLE_DELIVERY", "ROLE_ADMIN"
                )

                // Delivery partner operations
                .requestMatchers("/api/v1/delivery/**").hasAnyAuthority("ROLE_DELIVERY", "ROLE_ADMIN")

                // Admin Delivery endpoints
                .requestMatchers("/api/v1/admin/delivery/**").hasAnyAuthority("ROLE_ADMIN")

                // Shipping & Tracking endpoints (Customer, Seller, Admin)
                .requestMatchers("/api/v1/customer/shipments/**").hasAnyAuthority(
                    "ORDER_READ", "ROLE_CUSTOMER", "ROLE_ADMIN"
                )
                .requestMatchers("/api/v1/shipments/seller/**").hasAnyAuthority(
                    "SELLER_ORDER_MANAGE", "ROLE_SELLER", "ROLE_ADMIN"
                )
                .requestMatchers("/api/v1/admin/shipments/**").hasAnyAuthority("ROLE_ADMIN")
                .requestMatchers("/api/v1/shipments/**").hasAnyAuthority(
                    "ORDER_READ", "SELLER_ORDER_MANAGE", "ROLE_CUSTOMER", "ROLE_SELLER", "ROLE_DELIVERY", "ROLE_ADMIN"
                )

                // Admin Payment & Refund endpoints
                .requestMatchers("/api/v1/admin/payments/**", "/api/v1/admin/refunds/**").hasAnyAuthority(
                    "PAYMENT_MANAGE", "REFUND_MANAGE", "ROLE_ADMIN"
                )

                // Seller Reviews
                .requestMatchers("/api/v1/seller/reviews/**").hasAnyAuthority(
                    "REVIEW_READ", "SELLER_REVIEW_REPLY", "ROLE_SELLER", "ROLE_ADMIN"
                )

                // Admin Reviews & Moderation
                .requestMatchers("/api/v1/admin/reviews/**").hasAnyAuthority(
                    "REVIEW_MODERATE", "REVIEW_READ", "ROLE_ADMIN"
                )

                // Notification Management (Phase 15A)
                .requestMatchers("/api/v1/customer/notifications/**", "/api/v1/customer/notification-preferences/**").hasAnyAuthority(
                    "ROLE_CUSTOMER", "ROLE_ADMIN"
                )
                .requestMatchers("/api/v1/seller/notifications/**", "/api/v1/seller/notification-preferences/**").hasAnyAuthority(
                    "ROLE_SELLER", "ROLE_ADMIN"
                )
                .requestMatchers("/api/v1/admin/notifications/**").hasAnyAuthority(
                    "ROLE_ADMIN"
                )

                // Customer Support & Ticket Management (Phase 15B)
                .requestMatchers("/api/v1/customer/tickets/**").hasAnyAuthority(
                    "ROLE_CUSTOMER", "ROLE_ADMIN"
                )
                .requestMatchers("/api/v1/seller/tickets/**", "/api/v1/seller/ai/**", "/api/v1/seller/analytics/**").hasAnyAuthority(
                    "ROLE_SELLER", "ROLE_ADMIN"
                )
                .requestMatchers("/api/v1/delivery/tickets/**").hasAnyAuthority(
                    "ROLE_DELIVERY", "ROLE_ADMIN"
                )
                .requestMatchers("/api/v1/admin/support/**", "/api/v1/admin/ai/**", "/api/v1/admin/analytics/**").hasAnyAuthority(
                    "ROLE_ADMIN"
                )

                // Administrative endpoints require administrative authority
                .requestMatchers("/api/v1/admin/categories/**").hasAnyAuthority("CATEGORY_MANAGE", "ROLE_ADMIN")
                .requestMatchers("/api/v1/admin/brands/**").hasAnyAuthority("BRAND_MANAGE", "ROLE_ADMIN")
                .requestMatchers("/api/v1/admin/products/**").hasAnyAuthority("PRODUCT_MODERATE", "ROLE_ADMIN")
                .requestMatchers("/api/v1/admin/**").hasAnyAuthority("SELLER_MANAGE", "CATEGORY_MANAGE", "BRAND_MANAGE", "PRODUCT_MODERATE", "INVENTORY_AUDIT", "PAYMENT_MANAGE", "REFUND_MANAGE", "ROLE_ADMIN")

                // All other application requests require authentication
                .anyRequest().authenticated()
            )

            // 6. Register Rate Limiting Filter before LogoutFilter (executing after CORS, before JWT authentication)
            .addFilterBefore(rateLimitingFilter, LogoutFilter.class)
            // 7. Register JWT Authentication Filter before Spring's UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(!origins.contains("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
