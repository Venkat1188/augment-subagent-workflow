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
    @SuppressWarnings("java:S112") // Spring Security HttpSecurity.build() declares a checked Exception; no narrower type is available from the framework.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // S4502 — CSRF disabled intentionally: stateless API using HTTP Basic on every request.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // CWE-693 — explicit HTTP security headers for a banking API (SAST-05)
            .headers(headers -> headers
                .contentTypeOptions(opt -> {})                       // X-Content-Type-Options: nosniff
                .frameOptions(frame -> frame.deny())                 // X-Frame-Options: DENY
                .httpStrictTransportSecurity(hsts -> hsts            // HSTS: 1 year + subdomains
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000))
                .contentSecurityPolicy(csp ->                        // CSP: API-only, no resources
                    csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
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
