package com.omnichat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for Config Server as it's a backend service
            .authorizeHttpRequests(auth -> auth
                // Allow actuator health checks without auth for Kubernetes/Docker probes
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                // All other endpoints (including config fetches, encrypt/decrypt, refresh) require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults()); // Enable Basic Authentication

        return http.build();
    }
}
