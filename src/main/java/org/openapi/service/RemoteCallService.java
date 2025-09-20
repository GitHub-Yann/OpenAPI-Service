package org.openapi.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Service
public class RemoteCallService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteCallService.class);

    private WebClient webClient;

    // 服务映射配置 - 实际项目中应该从配置文件读取
    public static Map<String, String> SERVICE_MAPPING = new ConcurrentHashMap<String, String>(); 

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8080") // 可以根据需要修改基础URL
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    /**
     * 获取配置好的WebClient实例
     * @return WebClient实例
     */
    public WebClient getWebClient() {
        return this.webClient;
    }

    private HttpHeaders getHeaders(ServerWebExchange exchange) {
        HttpHeaders headers = new HttpHeaders();
        exchange.getRequest().getHeaders().forEach((name, values) -> {
            if(!"host".equalsIgnoreCase(name)){
                values.forEach(value -> headers.add(name, value));
            }
        });
        return headers;
    }

    /**
     * 代理处理方法 - 完整透传请求和响应（包括状态码和响应头）
     * @param exchange ServerWebExchange对象
     * @param targetUrl 目标服务URL
     * @return 完整的ResponseEntity（包含状态码、响应头、响应体）
     */
    public Mono<ResponseEntity<?>> processInvoke(ServerWebExchange exchange,String requestUniqueId) {
        HttpMethod method = exchange.getRequest().getMethod();
        HttpHeaders headers = this.getHeaders(exchange);
        // 生成转发地址
        String targetUrl = recognizeTargetUrl(exchange);
        LOGGER.info("[{}], new url {}", requestUniqueId, targetUrl);

        if(!StringUtils.hasText(targetUrl)){
            return Mono.just(ResponseEntity.status(404).body("{\"error\":\"OpenAPI - Resource Not Found\"}")); 
        }
        // 读取请求体
        return exchange.getRequest().getBody()
                .cast(DataBuffer.class)
                .reduce(DataBuffer::write)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes);
                })
                .defaultIfEmpty("") // 如果没有请求体，使用空字符串
                .flatMap(requestBody -> {
                    // 根据HTTP方法转发请求，返回完整的ResponseEntity
                    switch (method) {
                        case GET:
                            return getWithResponseEntity(targetUrl, headers);
                        case POST:
                            return postWithResponseEntity(targetUrl, requestBody, headers);
                        case PUT:
                            return putWithResponseEntity(targetUrl, requestBody, headers);
                        case DELETE:
                            return requestBody.isEmpty() ? 
                                deleteWithResponseEntity(targetUrl, headers) : 
                                deleteWithBodyResponseEntity(targetUrl, requestBody, headers);
                        default:
                            return Mono.just(ResponseEntity.status(405).body("{\"error\":\"Method Not Allowed\"}"));
                    }
                });
    }

    /**
     * <p>识别目标URL</p>
     * <ol>
     *   <li>规范请求path，后端服务的接口path统一都是 /api/v1 开头</li>
     *   <li>openAPI服务在接收请求的时候统一采用 /api/service-a/v1 开头，这样就可以通过识别path第二层中的内容来确认转发到什么服务上去</li>
     *   <li>因此需要配置服务地址的映射</li>
     * </ol>
     * 
     * <p>配置格式：</p>
     * <pre>
     * {
     *     "/api/service-a/**": "http://localhost:8081",
     *     "/api/service-b/**": "http://localhost:8082",
     *     "/api/service-c/**": "http://localhost:8083"
     * }
     * </pre>
     * 
     * @param exchange ServerWebExchange对象
     * @return 目标URL
     */
    private String recognizeTargetUrl(ServerWebExchange exchange) {
        String requestPath = exchange.getRequest().getURI().getPath();
        if (exchange.getRequest().getURI().getQuery() != null) {
            requestPath += "?" + exchange.getRequest().getURI().getQuery();
        }
    
        // 遍历服务映射配置，找到匹配的服务
        for (Map.Entry<String, String> entry : SERVICE_MAPPING.entrySet()) {
            String pattern = entry.getKey();
            String targetBaseUrl = entry.getValue();
            
            // 将 /api/service-a/** 转换为正则表达式匹配
            String regexPattern = pattern.replace("/**", "/.*");
            
            if (requestPath.matches(regexPattern)) {
                // 提取服务名称，例如从 "/api/service-a/**" 提取 "service-a"
                String serviceName = extractServiceName(pattern);
                
                // 转换路径：/api/service-a/v1/list -> /api/v1/list
                String targetPath = requestPath.replaceFirst("/api/" + serviceName, "/api");
                
                // 构建目标URL
                return targetBaseUrl + targetPath;
            }
        }
        return "";
    }

    /**
     * 从路径模式中提取服务名称
     * 例如：从 "/api/service-a/**" 提取 "service-a"
     * 
     * @param pattern 路径模式
     * @return 服务名称
     */
    private String extractServiceName(String pattern) {
        // 移除 /api/ 前缀和 /** 后缀
        String servicePart = pattern.replace("/api/", "").replace("/**", "");
        return servicePart;
    }

    /**
     * GET请求 - 返回ResponseEntity（包含状态码和响应头）
     * @param url 请求URL
     * @param headers 请求头
     * @return 完整的ResponseEntity
     */
    public Mono<ResponseEntity<String>> getWithResponseEntity(String url, HttpHeaders headers) {
        return webClient.get()
                .uri(url)
                .headers(httpHeaders -> {
                    if (headers != null) {
                        httpHeaders.addAll(headers);
                    }
                })
                .exchangeToMono(response -> {
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> ResponseEntity.status(response.statusCode())
                                    .headers(response.headers().asHttpHeaders())
                                    .body(body));
                });
    }

    /**
     * POST请求 - 返回ResponseEntity（包含状态码和响应头）
     * @param url 请求URL
     * @param requestBody 请求体
     * @param headers 请求头
     * @return 完整的ResponseEntity
     */
    public Mono<ResponseEntity<String>> postWithResponseEntity(String url, Object requestBody, HttpHeaders headers) {
        return webClient.post()
                .uri(url)
                .headers(httpHeaders -> {
                    if (headers != null) {
                        httpHeaders.addAll(headers);
                    }
                })
                .bodyValue(requestBody != null ? requestBody : "")
                .exchangeToMono(response -> {
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> ResponseEntity.status(response.statusCode())
                                    .headers(response.headers().asHttpHeaders())
                                    .body(body));
                });
    }

    /**
     * PUT请求 - 返回ResponseEntity（包含状态码和响应头）
     * @param url 请求URL
     * @param requestBody 请求体
     * @param headers 请求头
     * @return 完整的ResponseEntity
     */
    public Mono<ResponseEntity<String>> putWithResponseEntity(String url, Object requestBody, HttpHeaders headers) {
        return webClient.put()
                .uri(url)
                .headers(httpHeaders -> {
                    if (headers != null) {
                        httpHeaders.addAll(headers);
                    }
                })
                .bodyValue(requestBody != null ? requestBody : "")
                .exchangeToMono(response -> {
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> ResponseEntity.status(response.statusCode())
                                    .headers(response.headers().asHttpHeaders())
                                    .body(body));
                });
    }

    /**
     * DELETE请求 - 返回ResponseEntity（包含状态码和响应头）
     * @param url 请求URL
     * @param headers 请求头
     * @return 完整的ResponseEntity
     */
    public Mono<ResponseEntity<String>> deleteWithResponseEntity(String url, HttpHeaders headers) {
        return webClient.delete()
                .uri(url)
                .headers(httpHeaders -> {
                    if (headers != null) {
                        httpHeaders.addAll(headers);
                    }
                })
                .exchangeToMono(response -> {
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> ResponseEntity.status(response.statusCode())
                                    .headers(response.headers().asHttpHeaders())
                                    .body(body));
                });
    }

    /**
     * DELETE请求带请求体 - 返回ResponseEntity（包含状态码和响应头）
     * @param url 请求URL
     * @param requestBody 请求体
     * @param headers 请求头
     * @return 完整的ResponseEntity
     */
    public Mono<ResponseEntity<String>> deleteWithBodyResponseEntity(String url, Object requestBody, HttpHeaders headers) {
        return webClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(url)
                .headers(httpHeaders -> {
                    if (headers != null) {
                        httpHeaders.addAll(headers);
                    }
                })
                .bodyValue(requestBody != null ? requestBody : "")
                .exchangeToMono(response -> {
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> ResponseEntity.status(response.statusCode())
                                    .headers(response.headers().asHttpHeaders())
                                    .body(body));
                });
    }
}
