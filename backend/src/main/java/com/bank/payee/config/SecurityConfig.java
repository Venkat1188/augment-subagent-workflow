package com.bank.payee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the payee MFA API.
 * All endpoints under /api/payees/** require HTTP Basic authentication.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // S4502 — CSRF disabled intentionally: this API is stateless (no browser sessions,
            // no cookies used for authentication). All requests require HTTP Basic credentials
            // on every call. CSRF attacks require an authenticated session cookie, which is
            // absent here. Ensure TLS is enforced at the load-balancer/ingress layer.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/payees/initiate-mfa").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/payees/verify-otp").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/payees").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/payees/**").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {});
        return http.build();
    }
}
