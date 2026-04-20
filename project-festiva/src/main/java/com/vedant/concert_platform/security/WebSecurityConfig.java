package com.vedant.concert_platform.security;

import org.springframework.http.HttpMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // === EXPLICIT PUBLIC ENDPOINTS ONLY ===
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/mfa/verify").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/reset-password").permitAll()
                        // Fix #9: OTP login endpoint must be public
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login/otp/request").permitAll()

                        // Fix #7: Remove the wildcard /api/v1/auth/** permitAll — it exposed
                        // mfa/enable, mfa/disable, and change-password without authentication.
                        // All other auth endpoints now require authentication via anyRequest().authenticated() below.

                        // Public concert browsing
                        .requestMatchers(HttpMethod.GET, "/api/v1/concerts", "/api/v1/concerts/**").permitAll()

                        // Role-protected endpoints
                        .requestMatchers("/api/v1/producer/**").hasRole("PRODUCER")
                        .requestMatchers("/api/v1/promoter/**").hasRole("PROMOTER")
                        .requestMatchers("/api/v1/organizer/**").hasRole("ORGANIZER")
                        .requestMatchers("/api/v1/bookings/**").hasRole("ATTENDEE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/feedback/**").permitAll()
                        .requestMatchers("/api/v1/feedback/**").hasRole("ATTENDEE")

                        // Everything else (including remaining /api/v1/auth/** endpoints) requires JWT
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
