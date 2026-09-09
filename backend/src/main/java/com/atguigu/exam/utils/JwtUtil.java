package com.atguigu.exam.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "your-256-bit-secret-key-for-jwt-token-generation-and-validation";
    // token过期时间
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    /**
     * 获取签名密钥
     * 将字符串密钥转换为 HMAC-SHA 算法所需的 SecretKey 对象
     *
     * @return SecretKey 签名密钥对象
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * 生成 JWT Token
     * 根据用户ID和用户名生成包含用户信息的JWT令牌，有效期为24小时
     *
     * @param userId   用户ID，存储在token的claims中
     * @param username 用户名，同时作为subject和存储在claims中
     * @return 生成的JWT token字符串
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 JWT Token
     * 验证token的签名并解析出payload中的claims信息
     * 如果token无效或已过期，会抛出异常
     *
     * @param token 要解析的JWT token字符串
     * @return Claims 对象，包含token中的所有声明信息
     * @throws io.jsonwebtoken.JwtException 当token无效、过期或格式错误时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户ID
     * 解析token并从claims中提取userId字段
     *
     * @param token JWT token字符串
     * @return 用户ID，如果不存在则返回null
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取用户名
     * 解析token并从subject字段中提取用户名
     *
     * @param token JWT token字符串
     * @return 用户名，如果不存在则返回null
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 检查 Token 是否已过期
     * 解析token并比较expiration时间与当前时间
     *
     * @param token JWT token字符串
     * @return true-已过期，false-未过期
     */
    public boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().before(new Date());
    }

    /**
     * 验证 Token 的有效性
     * 综合检查token中的用户名是否匹配以及token是否未过期
     *
     * @param token    JWT token字符串
     * @param username 预期的用户名，用于校验token中的用户名是否一致
     * @return true-验证通过，false-验证失败（用户名不匹配或token已过期）
     */
    public boolean validateToken(String token, String username) {
        String tokenUsername = getUsernameFromToken(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }
}