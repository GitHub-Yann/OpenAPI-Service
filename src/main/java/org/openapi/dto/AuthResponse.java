package org.openapi.dto;

import java.time.LocalDateTime;

/**
 * JWT Token生成响应DTO
 */
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn; // 过期时间（秒）
    private LocalDateTime issuedAt; // 签发时间
    private LocalDateTime expiresAt; // 过期时间
    private String appId;
    private String appName;

    public AuthResponse() {
        this.issuedAt = LocalDateTime.now();
    }

    public AuthResponse(String token, Long expiresIn, String appId, String appName) {
        this();
        this.token = token;
        this.expiresIn = expiresIn;
        this.appId = appId;
        this.appName = appName;
        this.expiresAt = this.issuedAt.plusSeconds(expiresIn);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                ", appId='" + appId + '\'' +
                ", appName='" + appName + '\'' +
                '}';
    }
}