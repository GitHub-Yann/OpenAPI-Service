package org.openapi.filter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class TestFilter implements WebFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestFilter.class);

    private final WebClient webClient;

    public TestFilter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String reqId = UUID.randomUUID().toString();
        String path = exchange.getRequest().getURI().getPath();
        HttpHeaders headers = this.getHeaders(exchange,reqId);
        String method = exchange.getRequest().getMethod().name();

        LOGGER.info("[{}] Request {}, uri: {}", reqId,method,exchange.getRequest().getURI());
        // 获取请求体
        return exchange.getRequest()
                .getBody()
                .collectList()
                .flatMap(dataBuffers -> {
                    // 将DataBuffer转换为字符串
                    StringBuilder requestBody = new StringBuilder();
                    dataBuffers.forEach(buffer -> {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        requestBody.append(new String(bytes, StandardCharsets.UTF_8));
                    });
                    
                    // 修改请求体
                    String modifiedBody = modifyRequestBody(requestBody.toString(),reqId);
                    LOGGER.info("[{}] Modified request body: {}", reqId,modifiedBody);
                    
                    // 使用WebClient发送请求
                    return webClient
                            .post()
                            .uri("http://localhost:8000"+path) // 替换为实际的目标服务URL
                            .headers(httpHeaders->{
                                if (headers != null) {
                                    httpHeaders.addAll(headers);
                                }
                            })
                            .header("content-type", "application/json")
                            .bodyValue(modifiedBody)
                            .retrieve()
                            .bodyToMono(String.class)
                            .doOnNext(responseBody -> {
                                // 打印响应体，不做修改
                                LOGGER.info("[{}] Response body: {}",reqId, responseBody);
                            })
                            .flatMap(responseBody -> {
                                // 将响应写回客户端
                                DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
                                DataBuffer dataBuffer = bufferFactory.wrap(responseBody.getBytes(StandardCharsets.UTF_8));
                                // exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                                this.getRespHeaders(exchange, reqId);
                                return exchange.getResponse().writeWith(Mono.just(dataBuffer));
                            })
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                // 处理WebClient错误
                                LOGGER.error("[{}] WebClient error: {}", reqId,ex.getMessage());
                                exchange.getResponse().setStatusCode(ex.getStatusCode());
                                return exchange.getResponse().setComplete();
                            });
                });
    }

    private String modifyRequestBody(String originalBody,String reqId) {
        LOGGER.info("[{}] originalBody: {}", reqId,originalBody);
        // 在这里实现你的请求体修改逻辑
        // 示例：添加额外字段
        // return "{\"vvvvvvvv\":\""+Math.random()+"\"}"; // 返回修改后的请求体
        return originalBody;
    }

    private HttpHeaders getHeaders(ServerWebExchange exchange,String reqId) {
        HttpHeaders headers = new HttpHeaders();
        exchange.getRequest().getHeaders().forEach((name, values) -> {
            LOGGER.info("[{}] original header {}={}",reqId,name,values);
            if(!"host".equalsIgnoreCase(name)){
                values.forEach(value -> headers.add(name, value));
            }
        });
        return headers;
    }
    
    private void getRespHeaders(ServerWebExchange exchange,String reqId) {
        exchange.getResponse().getHeaders().forEach((name, values) -> {
            LOGGER.info("[{}] response header {}={}",reqId,name,values);
        });
    }
}
