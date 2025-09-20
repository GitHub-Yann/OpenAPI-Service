package org.openapi.filter;

import org.openapi.common.BaseResponse;
import org.openapi.common.ConstantsHub;
import org.openapi.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * API认证拦截器
 * 对特定路径进行认证检查，支持JWT Token和API Key两种方式
 */
@Component
@Order(0) // 在GlobalWebFilter之后执行
public class AuthenticationFilter implements WebFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);
    private final ObjectMapper objectMapper;
    
    @Autowired
    private JwtService jwtService;
    
    public AuthenticationFilter() {
        this.objectMapper = new ObjectMapper();
        // 配置ObjectMapper使用UTF-8编码
        this.objectMapper.getFactory().setCharacterEscapes(null);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 需要认证的路径模式（排除认证相关接口）
        if (!path.startsWith("/api/v1/") && !path.startsWith("/auth/")) {
            return authenticateRequest(exchange, chain);
        }

        // 不需要认证的路径直接通过
        return chain.filter(exchange);
    }

    /**
     * 执行认证检查
     */
    private Mono<Void> authenticateRequest(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestUniqueId = (String) exchange.getAttributes().get(ConstantsHub.REQ_UNIQUE_ID);
        
        // 获取认证头
        String authHeader = request.getHeaders().getFirst("Authorization");
        
        // 认证验证
        if (isValidAuth(authHeader,requestUniqueId)) {
            LOGGER.info("[{}]认证成功 - Path: {}",requestUniqueId, request.getURI().getPath());
            
            // 在请求头中添加用户信息
            ServerHttpRequest mutatedRequest = addUserInfoToRequest(request, authHeader,requestUniqueId);
            
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();
                    
            return chain.filter(mutatedExchange);
        } else {
            LOGGER.warn("[{}]认证失败 - Path: {} - IP: {}", 
                    requestUniqueId,
                    request.getURI().getPath(), 
                    getClientIp(request));
            return handleAuthenticationFailure(exchange);
        }
    }

    /**
     * 验证认证信息
     */
    private boolean isValidAuth(String authHeader,String requestUniqueId) {
        // 验证JWT Token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtService.validateToken(token);
            } catch (Exception e) {
                LOGGER.debug("[{}]JWT Token验证失败: {}",requestUniqueId, e.getMessage());
                return false;
            }
        }
        
        return false;
    }

    /**
     * 在请求头中添加用户信息
     */
    private ServerHttpRequest addUserInfoToRequest(ServerHttpRequest request, String authHeader,String requestUniqueId) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String appId = jwtService.getAppIdFromToken(token);
                String appName = jwtService.getAppNameFromToken(token);
                
                return request.mutate()
                        .header("X-User-Id", appId)
                        .header("X-User-Role", "app")
                        .header("X-App-Name", appName)
                        .build();
            } catch (Exception e) {
                LOGGER.debug("[{}]解析Token信息失败: {}",requestUniqueId, e.getMessage());
            }
        }
        
        // 默认情况
        return request.mutate()
                .header("X-User-Id", "anonymous")
                .header("X-User-Role", "guest")
                .build();
    }

    /**
     * 处理认证失败
     */
    private Mono<Void> handleAuthenticationFailure(ServerWebExchange exchange) {
        return createErrorResponse(exchange, HttpStatus.UNAUTHORIZED, 401, "认证失败，请提供有效的认证信息");
    }

    /**
     * 创建错误响应（统一处理UTF-8编码）
     */
    private Mono<Void> createErrorResponse(ServerWebExchange exchange, HttpStatus status, int errorCode, String errorMessage) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        
        // 设置正确的Content-Type和字符编码
        response.getHeaders().set("Content-Type", "application/json;charset=UTF-8");
        // 额外设置字符编码响应头确保兼容性
        response.getHeaders().set("Accept-Charset", "UTF-8");
        // 设置Cache-Control避免缓存错误响应
        response.getHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");

        BaseResponse<String> errorResponse = BaseResponse.error(errorCode, errorMessage);
        
        try {
            String responseBody = objectMapper.writeValueAsString(errorResponse);
            // 明确指定UTF-8编码转换字节数组
            DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes("UTF-8"));
            
            LOGGER.debug("错误响应内容: {}", responseBody);
            return response.writeWith(Flux.just(buffer));
        } catch (JsonProcessingException e) {
            LOGGER.error("序列化错误响应失败", e);
            return response.setComplete();
        } catch (java.io.UnsupportedEncodingException e) {
            LOGGER.error("UTF-8编码转换失败", e);
            return response.setComplete();
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        return request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
}