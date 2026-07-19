package com.omnichat.discovery;

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
            // Disable CSRF to allow Eureka Clients to register without CSRF tokens
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Allow Actuator health checks to be publicly accessible for container orchestrators (e.g., K8s)
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                // All other requests (including Eureka Dashboard and Client registration) require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults()); // Enable Basic Authentication

        return http.build();
    }
}
