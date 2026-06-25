package com.apix.memory.config;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 认证过滤器 — 对需要认证的接口进行 token 验证。
 *
 * 跳过 /user/login, /user/register, /health 等公开接口。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtConfig jwtConfig;

    private static final java.util.Set<String> PUBLIC_PATHS = java.util.Set.of(
        "/user/login", "/user/register", "/health", "/api/v1/get_models_list",
        "/auth/login", "/auth/register", "/auth/ensure_user", "/auth/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 公开路径跳过认证
        for (String pub : PUBLIC_PATHS) {
            if (path.contains(pub)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // 从 Header 中提取 token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"Missing or invalid token\"}");
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtConfig.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"Token expired or invalid\"}");
            return;
        }

        // 将用户信息存入 request attribute
        Claims claims = jwtConfig.parseToken(token);
        request.setAttribute("userUid", claims.getSubject());
        request.setAttribute("username", claims.get("username"));

        filterChain.doFilter(request, response);
    }
}
