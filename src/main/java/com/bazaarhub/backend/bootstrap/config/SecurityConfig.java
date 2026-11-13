package com.bazaarhub.backend.bootstrap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration for BazaarHub Backend composition root.
 *
 * <p>Security Policy:
 * <ul>
 *   <li>Permits unauthenticated access strictly to the minimal operational health endpoint ({@code /actuator/health})
 *       to enable Docker HEALTHCHECK and reverse proxy monitoring without exposing infrastructure metadata or credentials.</li>
 *   <li>All other Actuator (e.g. {@code /actuator/info}) and application endpoints require authentication ({@code anyRequest().authenticated()}).</li>
 *   <li>Maintains standard HTTP Basic and Form Login defaults for authenticated access.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(withDefaults())
            .formLogin(withDefaults());

        return http.build();
    }
}
