package org.openapi.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.openapi.service.RemoteCallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebFluxConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebFluxConfig.class);

    public WebFluxConfig() {
    }

    @PostConstruct
    public void init() {
        LOGGER.info("初始化......");

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> triggerTheServiceDiscoveryTask(), 5,
                600, TimeUnit.SECONDS);
    }

    private void triggerTheServiceDiscoveryTask() {
        LOGGER.info("触发服务发现任务......");
        try{
            // TODO: 拉取服务列表，这边暂时使用的是模拟数据，后期可以对接eureka，nacos等服务注册中心
            Map<String, String> tmp = new ConcurrentHashMap<String, String>(); 
            tmp.put("/api/service-a/**", "http://192.168.2.51:8081");
            tmp.put("/api/service-b/**", "http://localhost:8081");
            tmp.put("/api/service-c/**", "http://localhost:8082");

            RemoteCallService.SERVICE_MAPPING = tmp;
        }catch(Exception e){
            LOGGER.error("触发服务发现任务异常: {}", e.getMessage());
        }
    }
}