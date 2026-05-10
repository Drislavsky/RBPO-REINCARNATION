package com.example.autoservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/signatures", "/api/signatures/increment").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/signatures/by-ids").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/signatures", "/api/signatures/file", "/api/signatures/presigned-urls").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/signatures/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/signatures/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/signatures/*/history", "/api/signatures/*/audit").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/binary/signatures/full", "/api/binary/signatures/increment").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/binary/signatures/by-ids").hasAnyRole("ADMIN", "USER")

                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/license/**").hasAnyRole("ADMIN", "USER")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}