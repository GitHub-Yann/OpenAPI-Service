package org.openapi.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;

import org.openapi.common.BaseResponse;
import org.openapi.dto.AuthRequest;
import org.openapi.dto.AuthResponse;
import org.openapi.entity.AppCredentials;
import org.openapi.service.AppService;
import org.openapi.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 * 认证控制器
 * 提供JWT token生成和管理接口
 */
@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AppService appService;

    @Autowired
    private JwtService jwtService;

    /**
     * 生成JWT Token接口
     * 
     * @param authRequest 包含appId和appSecret的认证请求
     * @return JWT token响应
     */
    @PostMapping("/token")
    public Mono<ResponseEntity<BaseResponse<AuthResponse>>> generateToken(@Valid @RequestBody AuthRequest authRequest) {
        return Mono.fromCallable(() -> {
            LOGGER.info("收到Token生成请求 - appId: {}", authRequest.getAppId());
            
            try {
                // 验证应用凭证
                Optional<AppCredentials> credentialsOpt = appService.validateCredentials(
                    authRequest.getAppId(), 
                    authRequest.getAppSecret()
                );

                if (!credentialsOpt.isPresent()) {
                    LOGGER.warn("应用凭证验证失败 - appId: {}", authRequest.getAppId());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.error(401, "应用凭证验证失败，请检查appId和appSecret"));
                }

                AppCredentials credentials = credentialsOpt.get();

                // 生成JWT token
                String token = jwtService.generateToken(credentials.getAppId(), credentials.getAppName());
                
                // 构建响应
                AuthResponse authResponse = new AuthResponse(
                    token,
                    jwtService.getJwtExpiration(),
                    credentials.getAppId(),
                    credentials.getAppName()
                );

                LOGGER.info("JWT Token生成成功 - appId: {}, appName: {}, 有效期: {}天", 
                    credentials.getAppId(), 
                    credentials.getAppName(),
                    jwtService.getJwtExpiration() / 86400);

                return ResponseEntity.ok(BaseResponse.OK("Token生成成功", authResponse));

            } catch (Exception e) {
                LOGGER.error("Token生成异常 - appId: {}, error: {}", 
                    authRequest.getAppId(), e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error(500, "Token生成失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 验证JWT Token接口
     * 
     * @param token JWT token
     * @return 验证结果
     */
    @PostMapping("/validate")
    public Mono<ResponseEntity<BaseResponse<Map<String, Object>>>> validateToken(@RequestParam String token) {
        return Mono.fromCallable(() -> {
            LOGGER.info("收到Token验证请求");
            
            try {
                boolean isValid = jwtService.validateToken(token);
                
                if (!isValid) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.error(401, "Token无效或已过期"));
                }

                // 解析token信息
                String appId = jwtService.getAppIdFromToken(token);
                String appName = jwtService.getAppNameFromToken(token);
                long remainingTime = jwtService.getTokenRemainingTime(token);
                boolean nearExpiry = jwtService.isTokenNearExpiry(token);

                Map<String, Object> result = new HashMap<>();
                result.put("valid", true);
                result.put("appId", appId);
                result.put("appName", appName);
                result.put("remainingTime", remainingTime);
                result.put("nearExpiry", nearExpiry);
                result.put("validatedAt", LocalDateTime.now());

                LOGGER.info("Token验证成功 - appId: {}, 剩余时间: {}秒", appId, remainingTime);

                return ResponseEntity.ok(BaseResponse.OK("Token验证成功", result));

            } catch (Exception e) {
                LOGGER.warn("Token验证失败: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error(401, "Token验证失败: " + e.getMessage()));
            }
        });
    }
}