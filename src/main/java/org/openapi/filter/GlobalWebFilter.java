package org.openapi.filter;

import java.time.LocalDateTime;
import java.util.UUID;

import org.openapi.common.BaseResponse;
import org.openapi.common.ConstantsHub;
import org.openapi.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * 全局WebFilter拦截器
 * 用于记录请求日志、处理跨域、添加响应头等
 */
@Component
@Order(-1) // 设置过滤器优先级，数值越小优先级越高
public class GlobalWebFilter implements WebFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalWebFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        String requestTime = LocalDateTime.now().format(Utils.FORMATTER);
        
        // 生成请求唯一ID
        String requestUniqueId = request.getHeaders().getFirst("X-Request-Unique-Id");
        if(StringUtils.hasText(requestUniqueId)){
            exchange.getAttributes().put(ConstantsHub.REQ_UNIQUE_ID, requestUniqueId);
        }else{
            requestUniqueId = UUID.randomUUID().toString();
            exchange.getAttributes().put(ConstantsHub.REQ_UNIQUE_ID, requestUniqueId);
        }
        String finalRequestUniqueId = requestUniqueId;

        // 记录请求信息
        LOGGER.info("request came [{}][{}] {} {} from {} - User-Agent: {}", 
                requestTime,
                finalRequestUniqueId,
                request.getMethod(), 
                request.getURI(), 
                getClientIp(request),
                request.getHeaders().getFirst("User-Agent"));

        // 添加自定义响应头
        response.getHeaders().add("X-Request-Time", requestTime);
        response.getHeaders().add("X-Service-Name", "openapi-service");
        
        // 处理跨域
        handleCors(request, response);
        
        // 继续过滤器链，并在完成后记录响应信息
        return chain.filter(exchange)
                .doOnSuccess(aVoid -> {
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    LOGGER.info("request done [{}][{}] {} {} - Status: {} - Duration: {}ms", 
                            requestTime,
                            finalRequestUniqueId,
                            request.getMethod(), 
                            request.getURI(),
                            response.getStatusCode(),
                            duration);
                })
                .onErrorResume(throwable -> {
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    LOGGER.error("request done [{}][{}] {} {} - Error: {} - Duration: {}ms", 
                            requestTime,
                            finalRequestUniqueId,
                            request.getMethod(), 
                            request.getURI(),
                            throwable.getMessage(),
                            duration);
                    
                    // 构造错误响应体
                    String errorResponse;
                    String errorMsg = "OpenAPI - 服务器内部错误";
                    int errorCode = 500;
                    try {
                        if(throwable instanceof ResponseStatusException){
                            errorMsg = "OpenAPI - "+((ResponseStatusException) throwable).getReason();
                            errorCode = ((ResponseStatusException) throwable).getStatus().value();
                        }
                        errorResponse = Utils.OBJECT_MAPPER.writeValueAsString(BaseResponse.ERROR(errorCode,errorMsg));
                    } catch (Exception e) {
                        errorResponse = BaseResponse.toErrorJsonString(500, "OpenAPI - 服务器内部错误[1]");
                    }
                    // 设置统一错误响应
                    response.setRawStatusCode(errorCode);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    // 写入响应
                    return response.writeWith(Mono.just(response.bufferFactory().wrap(errorResponse.getBytes())));
                });
    }

    /**
     * 处理跨域请求
     */
    private void handleCors(ServerHttpRequest request, ServerHttpResponse response) {
        response.getHeaders().add("Access-Control-Allow-Origin", "*");
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.getHeaders().add("Access-Control-Allow-Headers", "*");
        response.getHeaders().add("Access-Control-Max-Age", "3600");
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
}