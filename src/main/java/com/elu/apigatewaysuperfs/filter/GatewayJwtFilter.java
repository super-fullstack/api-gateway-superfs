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
    private static final List<String> PUBLIC_URLS = List.of(
            "/auth/login",
            "/auth/signup",
            "/actuator/health",
            "/auth/**",
            "/actuator/info",
            "/swagger-ui/**",
            "/v3/**"
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

//        System.out.println("INSIDE API AGTE WAY");

//        String path = req.getRequestURI();
//        System.out.println("THIS IS THE PATH " + path);
//        // 1) Skip public paths
//        if (PUBLIC_URLS.stream().anyMatch(path::startsWith)) {
//            System.out.println("THIS IS OKAY" + path);
//            chain.doFilter(req, res);
//            return;


        String path = req.getRequestURI();

        boolean isPublicPath = PUBLIC_URLS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (isPublicPath) {
            System.out.println("JWT Filter - Public path, skipping authentication: " + path);
            chain.doFilter(req, res);
            return;
        }

        // 2) Extract token
        String token = null;
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("jwt".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        // 3) Validate
        if (token == null || !jwtUtil.validateToken(token)) {
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        // 4) Build a minimal Authentication
        String email = jwtUtil.extractEmail(token);
        var auth = new UsernamePasswordAuthenticationToken(
                email, null,
                jwtUtil.extractRoles(token).stream() // assume you have this method
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList())
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(req, res);
    }
}
