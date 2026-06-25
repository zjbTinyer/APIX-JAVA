package com.apix.memory.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 配置 — 用于用户登录认证。
 *
 * 对标 Python 中的 JWT 实现（如果存在）。
 */
@Component
public class JwtConfig {

    @Value("${apix.jwt.secret:apix-default-jwt-secret-key-change-in-production}")
    private String secret;

    @Value("${apix.jwt.expire-hours:72}")
    private long expireHours;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT token。
     */
    public String generateToken(String userUid, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireHours * 3600_000);

        return Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setSubject(userUid)
            .claim("username", username)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * 解析 JWT token。
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * 验证 token 是否有效。
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从 token 中提取用户 UID。
     */
    public String getUserUidFromToken(String token) {
        return parseToken(token).getSubject();
    }
}
