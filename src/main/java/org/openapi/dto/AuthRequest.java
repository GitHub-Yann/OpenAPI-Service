package org.openapi.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * JWT Token生成请求DTO
 */
public class AuthRequest {

    @NotBlank(message = "appId不能为空")
    @Size(max = 32, message = "appId长度不能超过32字符")
    private String appId;

    @NotBlank(message = "appSecret不能为空")
    @Size(max = 64, message = "appSecret长度不能超过64字符")
    private String appSecret;

    public AuthRequest() {}

    public AuthRequest(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
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

    @Override
    public String toString() {
        return "AuthRequest{" +
                "appId='" + appId + '\'' +
                ", appSecret='***'" + // 隐藏敏感信息
                '}';
    }
}