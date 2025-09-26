package org.openapi.filter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.openapi.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import reactor.core.publisher.Flux;
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
        StringBuilder path = new StringBuilder(exchange.getRequest().getURI().getPath());
        if (exchange.getRequest().getURI().getQuery() != null) {
            path.append("?").append(exchange.getRequest().getURI().getQuery());
        }
        HttpHeaders headers = this.getHeaders(exchange,reqId);
        HttpMethod method = exchange.getRequest().getMethod();

        LOGGER.info("[{}] Request {}, uri: {}", reqId,method.name(),exchange.getRequest().getURI());
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
                    // LOGGER.info("[{}] Modified request body: {}", reqId,modifiedBody);
                    
                    // 使用WebClient发送请求
                    return processInvoke(exchange,method,path.toString(),modifiedBody,headers,reqId);
                });
    }
    private Mono<Void> processInvoke(ServerWebExchange exchange,HttpMethod method,String path,String modifiedBody,HttpHeaders headers,String reqId) {
        // 获取原始响应体
        switch (method) {
            case GET:
                return processGetInvoke(exchange,method,path,modifiedBody,headers,reqId);
            case POST:
                return processPostInvoke(exchange,method,path,modifiedBody,headers,reqId);
            case PUT:
                return processPutInvoke(exchange,method,path,modifiedBody,headers,reqId);
            case DELETE:
                return processDeleteInvoke(exchange,method,path,modifiedBody,headers,reqId);
            default:{
                DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
                DataBuffer dataBuffer = bufferFactory.wrap("{\"error\":\"Method Not Allowed\"}".getBytes(StandardCharsets.UTF_8));
                exchange.getResponse().setRawStatusCode(405);
                // 直接响应连接关闭，因为从wireshark看到一直有探测的包
                // 也可以设置netty的空闲超时时间
                exchange.getResponse().getHeaders().set("Connection", "close");
                return exchange.getResponse().writeWith(Mono.just(dataBuffer));
            }
        }
    }

    private Mono<Void> processGetInvoke(ServerWebExchange exchange,HttpMethod method,String path,String modifiedBody,HttpHeaders headers,String reqId) {
        // 获取原始响应体
        return webClient
                .get()
                .uri("http://127.0.0.1:8001"+path) // 替换为实际的目标服务URL
                .headers(httpHeaders->{
                    if (headers != null) {
                        httpHeaders.addAll(headers);
                    }
                })
                .exchangeToMono(response->{
                    return doExchange("doGET",response, exchange, method, path, modifiedBody, headers, reqId);
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    LOGGER.error("method=[{}], reqId=[{}], WebClient error: {}","doGET", reqId, ex.getMessage());
                    exchange.getResponse().setStatusCode(ex.getStatusCode());
                    return exchange.getResponse().setComplete();
                });
    }

    private Mono<Void> processPostInvoke(ServerWebExchange exchange,HttpMethod method,String path,String modifiedBody,HttpHeaders headers,String reqId) {
        // 获取原始响应体
        return webClient
                .post()
                .uri("http://127.0.0.1:8001"+path) // 替换为实际的目标服务URL
                .headers(httpHeaders->{
                    if (headers != null) {
                        httpHeaders.addAll(headers);
                    }
                })
                .header("content-type", "application/json")
                .bodyValue(modifiedBody)
                .exchangeToMono(response->{
                    return doExchange("doPOST",response, exchange, method, path, modifiedBody, headers, reqId);
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    LOGGER.error("method=[{}], reqId=[{}], WebClient error: {}","doPOST", reqId, ex.getMessage());
                    exchange.getResponse().setStatusCode(ex.getStatusCode());
                    return exchange.getResponse().setComplete();
                });
    }

    private Mono<Void> processPutInvoke(ServerWebExchange exchange,HttpMethod method,String path,String modifiedBody,HttpHeaders headers,String reqId) {
        // 获取原始响应体
        return webClient
                .put()
                .uri("http://127.0.0.1:8001"+path) // 替换为实际的目标服务URL
                .headers(httpHeaders->{
                    if (headers != null) {
                        httpHeaders.addAll(headers);
                    }
                })
                .header("content-type", "application/json")
                .bodyValue(modifiedBody)
                .exchangeToMono(response->{
                    return doExchange("doPUT",response, exchange, method, path, modifiedBody, headers, reqId);
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    LOGGER.error("method=[{}], reqId=[{}], WebClient error: {}","doPUT", reqId, ex.getMessage());
                    exchange.getResponse().setStatusCode(ex.getStatusCode());
                    return exchange.getResponse().setComplete();
                });
    }

    private Mono<Void> processDeleteInvoke(ServerWebExchange exchange,HttpMethod method,String path,String modifiedBody,HttpHeaders headers,String reqId) {
        // 获取原始响应体
        return webClient
                .delete()
                .uri("http://127.0.0.1:8001"+path) // 替换为实际的目标服务URL
                .headers(httpHeaders->{
                    if (headers != null) {
                        httpHeaders.addAll(headers);
                    }
                })
                .exchangeToMono(response->{
                    return doExchange("doDELETE",response, exchange, method, path, modifiedBody, headers, reqId);
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    LOGGER.error("method=[{}], reqId=[{}], WebClient error: {}","doDELETE", reqId, ex.getMessage());
                    exchange.getResponse().setStatusCode(ex.getStatusCode());
                    return exchange.getResponse().setComplete();
                });
    }

    private Mono<Void> doExchange(String action,ClientResponse response,ServerWebExchange exchange,HttpMethod method,String path,String modifiedBody,HttpHeaders headers,String reqId) { 
        // 先设置状态码
        exchange.getResponse().setStatusCode(response.statusCode());
        // 先获取后端返回的所有响应头
        HttpHeaders backendHeaders = response.headers().asHttpHeaders();
        
        // 检查响应的 Content-Type
        String contentType = response.headers().header("Content-Type")
                .stream().findFirst().orElse("");
        
        if (contentType.contains("text/event-stream")) {
            // 处理 SSE 流式响应
            LOGGER.info("method=[{}], reqId=[{}], Received SSE response",action,reqId);
            backendHeaders.forEach((headerName, headerValues)->{
                // 跳过一些可能冲突的头部，但保留业务相关的头部如 mcp-session-id
                if (!headerName.equalsIgnoreCase("transfer-encoding") && 
                    !headerName.equalsIgnoreCase("content-length") &&
                    !headerName.equalsIgnoreCase("content-encoding")) {                 
                    exchange.getResponse().getHeaders().put(headerName, headerValues);
                }
            });
            // 设置响应头
            exchange.getResponse().getHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponse().getHeaders().add("Cache-Control", "no-cache");
            exchange.getResponse().getHeaders().add("Connection", "keep-alive");
            // 打印响应头
            this.getRespHeaders(exchange, reqId);
            
            // 将 SSE 数据流直接转发给客户端，这边只是转发响应体，不包含响应头
            Flux<DataBuffer> sseFlux = response.bodyToFlux(DataBuffer.class)
                    .doOnNext(buffer -> {
                        // 可以在这里记录每个 SSE 事件
                        String data = StandardCharsets.UTF_8.decode(buffer.asByteBuffer()).toString();
                        LOGGER.debug("method=[{}], reqId=[{}], SSE data: {}",action, reqId, data);
                    })
                    .doOnError(error -> {
                        LOGGER.error("method=[{}], reqId=[{}], SSE stream error: {}",action, reqId, error.getMessage());
                    })
                    .doOnComplete(() -> {
                        LOGGER.info("method=[{}], reqId=[{}], SSE stream completed",action, reqId);
                    });
            return exchange.getResponse().writeWith(sseFlux);
        } else {
            // 处理普通响应
            LOGGER.info("method=[{}], reqId=[{}], Received normal response", reqId);
            
            // 复制响应头
            backendHeaders.forEach((headerName, headerValues) -> {
                if (!headerName.equalsIgnoreCase("transfer-encoding") && 
                    !headerName.equalsIgnoreCase("content-length") &&
                    !headerName.equalsIgnoreCase("content-encoding")) {
                    exchange.getResponse().getHeaders().put(headerName, headerValues);
                }
            });
            
            return response.bodyToMono(String.class)
                    .doOnNext(responseBody -> {
                        LOGGER.info("method=[{}], reqId=[{}], Response body: {}",action, reqId, responseBody);
                    })
                    .flatMap(responseBody -> {
                        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
                        DataBuffer buffer = bufferFactory.wrap(responseBody.getBytes(StandardCharsets.UTF_8));
                        return exchange.getResponse().writeWith(Mono.just(buffer));
                    });
        }
    }

    private String modifyRequestBody(String originalBody,String reqId) {
        String newBody="";
        // 在这里实现你的请求体修改逻辑
        try {
            ObjectNode newBodyNode = Utils.OBJECT_MAPPER.createObjectNode();
            JsonNode originalBodyNode = Utils.OBJECT_MAPPER.readTree(originalBody);
            // 在这里实现你的请求体修改逻辑
            // 示例：添加额外字段
            if(originalBodyNode!=null && originalBodyNode.isObject()) {
                newBodyNode.setAll((ObjectNode) originalBodyNode);
            }
            newBodyNode.put("newField1111", "newValue222222");
            // 返回修改后的请求体
            newBody = newBodyNode.toString();
        } catch (Exception e) {
            LOGGER.error("reqId=[{}] , EXP: {}",reqId, e.getMessage(),e);
            return originalBody;
        }
        LOGGER.info("reqId=[{}] originalBody: {}, newBody: {}", reqId,originalBody,newBody);
        return newBody;
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
