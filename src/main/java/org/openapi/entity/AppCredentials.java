package org.openapi.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 应用凭证实体类
 * 用于存储应用的appId和appSecret信息
 */
@Entity
@Table(name = "app_credentials")
public class AppCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app_id", unique = true, nullable = false, length = 32)
    private String appId;

    @Column(name = "app_secret", nullable = false, length = 64)
    private String appSecret;

    @Column(name = "app_name", length = 100)
    private String appName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "status")
    private Integer status = 1; // 1:启用, 0:禁用

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    // 构造函数
    public AppCredentials() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }

    public AppCredentials(String appId, String appSecret, String appName) {
        this();
        this.appId = appId;
        this.appSecret = appSecret;
        this.appName = appName;
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    @Override
    public String toString() {
        return "AppCredentials{" +
                "id=" + id +
                ", appId='" + appId + '\'' +
                ", appName='" + appName + '\'' +
                ", status=" + status +
                ", createdTime=" + createdTime +
                '}';
    }
}