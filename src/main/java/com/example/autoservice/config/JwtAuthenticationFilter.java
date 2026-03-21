package com.example.autoservice.config;

import com.example.autoservice.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public JwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        // 1. Пропускаем запросы авторизации сразу, чтобы не проверять токены там, где их нет
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        // 2. ПРОВЕРКА: Если заголовка нет или он не начинается с "Bearer ", просто идем дальше по цепочке.
        // Это предотвратит NullPointerException.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Теперь безопасно достаем токен
        String token = authHeader.substring(7);

        try {
            Claims claims = tokenService.validateAccessToken(token);

            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            if (username != null && role != null) {
                // Spring Security ожидает роли с префиксом ROLE_
                String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(username)
                        .password("") // Пароль здесь не нужен, так как аутентификация по токену
                        .authorities(authority)
                        .build();

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Устанавливаем пользователя в контекст Spring Security
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (Exception e) {
            // Если токен кривой или протух — логируем и идем дальше.
            // Spring сам вернет 403 на защищенные эндпоинты, так как контекст будет пустой.
            logger.warn("JWT Authentication failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}