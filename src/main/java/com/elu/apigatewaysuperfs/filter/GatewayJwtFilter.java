package com.elu.apigatewaysuperfs.filter;

import com.elu.apigatewaysuperfs.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GatewayJwtFilter extends OncePerRequestFilter {

    // Use Ant-style patterns with ** for matching all subpaths
    private static final List<String> PUBLIC_URLS = List.of(
            "/auth/login",
            "/auth/signup",
            "/auth/**",              // Matches all paths under /auth/
            "/actuator/**",          // Matches all actuator endpoints
            "/swagger-ui/**",        // Matches all swagger UI resources
            "/v3/**",                // Matches all OpenAPI docs
            "/error",                // Spring Boot error endpoint
            "/favicon.ico"           // Common browser request
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JwtUtil jwtUtil;

    public GatewayJwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();

        // Debug logging (remove in production)
        System.out.println("JWT Filter - Processing path: " + path);

        // 1) Skip public paths - using proper Ant pattern matching
        boolean isPublicPath = PUBLIC_URLS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (isPublicPath) {
            System.out.println("JWT Filter - Public path, skipping authentication: " + path);
            chain.doFilter(req, res);
            return;
        }

        // 2) Extract token from Authorization header or Cookie
        String token = extractToken(req);

        // 3) Validate token
        if (token == null) {
            System.out.println("JWT Filter - No token found for path: " + path);
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
            res.getWriter().write("{\"error\": \"No authentication token provided\"}");
            return;
        }

        if (!jwtUtil.validateToken(token)) {
            System.out.println("JWT Filter - Invalid token for path: " + path);
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
            res.getWriter().write("{\"error\": \"Invalid or expired token\"}");
            return;
        }

        // 4) Build Authentication and set in SecurityContext
        try {
            String email = jwtUtil.extractEmail(token);
            List<SimpleGrantedAuthority> authorities = jwtUtil.extractRoles(token).stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);

            System.out.println("JWT Filter - Authentication successful for: " + email);

        } catch (Exception e) {
            System.err.println("JWT Filter - Error processing token: " + e.getMessage());
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
            res.getWriter().write("{\"error\": \"Token processing failed\"}");
            return;
        }

        chain.doFilter(req, res);
    }

    private String extractToken(HttpServletRequest req) {
        // Try Authorization header first
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Fallback to cookie
        if (req.getCookies() != null) {
            for (Cookie cookie : req.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}