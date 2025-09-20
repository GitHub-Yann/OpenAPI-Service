package org.openapi.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具服务类
 * 负责JWT token的生成、解析和验证
 */
@Service
public class JwtService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret:myDefaultSecretKeyForOpenApiServiceThatIsLongEnoughForHmacSha256}")
    private String jwtSecret;

    @Value("${jwt.expiration:604800}") // 默认7天(7*24*60*60秒 = 604800)
    private Long jwtExpiration;

    @Value("${jwt.issuer:openapi-service}")
    private String jwtIssuer;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // 使用更安全的密钥生成方式
        if (jwtSecret.length() < 32) {
            LOGGER.warn("JWT密钥长度不足，使用默认密钥");
            jwtSecret = "myDefaultSecretKeyForOpenApiServiceThatIsLongEnoughForHmacSha256";
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        LOGGER.info("JWT服务初始化完成 - 过期时间: {}秒 ({}天)", jwtExpiration, jwtExpiration / 86400);
    }

    /**
     * 生成JWT Token
     */
    public String generateToken(String appId, String appName) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("appId", appId);
        claims.put("appName", appName);
        claims.put("type", "access_token");

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(appId)
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        LOGGER.info("JWT Token生成成功 - appId: {}, 过期时间: {}", appId, 
                LocalDateTime.ofInstant(expiryDate.toInstant(), ZoneId.systemDefault()));

        return token;
    }

    /**
     * 解析JWT Token
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            LOGGER.warn("JWT Token已过期: {}", e.getMessage());
            throw new RuntimeException("Token已过期", e);
        } catch (UnsupportedJwtException e) {
            LOGGER.warn("不支持的JWT Token: {}", e.getMessage());
            throw new RuntimeException("不支持的Token格式", e);
        } catch (MalformedJwtException e) {
            LOGGER.warn("JWT Token格式错误: {}", e.getMessage());
            throw new RuntimeException("Token格式错误", e);
        } catch (SecurityException e) {
            LOGGER.warn("JWT Token签名验证失败: {}", e.getMessage());
            throw new RuntimeException("Token签名验证失败", e);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("JWT Token参数错误: {}", e.getMessage());
            throw new RuntimeException("Token参数错误", e);
        }
    }

    /**
     * 验证JWT Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            LOGGER.debug("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Token中获取appId
     */
    public String getAppIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("appId", String.class);
    }

    /**
     * 从Token中获取应用名称
     */
    public String getAppNameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("appName", String.class);
    }

    /**
     * 检查Token是否即将过期（在1小时内过期）
     */
    public boolean isTokenNearExpiry(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            long timeUntilExpiry = expiration.getTime() - now.getTime();
            return timeUntilExpiry < 3600000; // 1小时 = 3600000毫秒
        } catch (Exception e) {
            return true; // 如果解析失败，认为即将过期
        }
    }

    /**
     * 获取Token的剩余有效时间（秒）
     */
    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            return Math.max(0, (expiration.getTime() - now.getTime()) / 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取JWT过期时间配置
     */
    public Long getJwtExpiration() {
        return jwtExpiration;
    }
}