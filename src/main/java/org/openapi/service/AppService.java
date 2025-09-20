package org.openapi.service;

import java.util.Optional;
import java.util.UUID;

import org.openapi.entity.AppCredentials;
import org.openapi.repository.AppCredentialsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 应用服务类
 * 负责应用凭证的管理和验证
 */
@Service
@Transactional
public class AppService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppService.class);

    @Autowired
    private AppCredentialsRepository appCredentialsRepository;

    /**
     * 验证应用凭证
     */
    public Optional<AppCredentials> validateCredentials(String appId, String appSecret) {
        LOGGER.debug("验证应用凭证 - appId: {}", appId);
        
        try {
            Optional<AppCredentials> credentials = appCredentialsRepository.validateCredentials(appId, appSecret);
            
            if (credentials.isPresent()) {
                LOGGER.info("应用凭证验证成功 - appId: {}, appName: {}", appId, credentials.get().getAppName());
            } else {
                LOGGER.warn("应用凭证验证失败 - appId: {}", appId);
            }
            
            return credentials;
        } catch (Exception e) {
            LOGGER.error("应用凭证验证异常 - appId: {}, error: {}", appId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 根据appId查找应用
     */
    public Optional<AppCredentials> findByAppId(String appId) {
        return appCredentialsRepository.findByAppId(appId);
    }

    /**
     * 根据appId查找启用状态的应用
     */
    public Optional<AppCredentials> findActiveByAppId(String appId) {
        return appCredentialsRepository.findByAppIdAndStatus(appId, 1);
    }

    /**
     * 创建新的应用凭证
     */
    public AppCredentials createApp(String appName, String description) {
        String appId = "app-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String appSecret = "secret-" + UUID.randomUUID().toString().replace("-", "") + 
                          UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        AppCredentials app = new AppCredentials(appId, appSecret, appName);
        app.setDescription(description);
        
        AppCredentials savedApp = appCredentialsRepository.save(app);
        LOGGER.info("新应用创建成功 - appId: {}, appName: {}", appId, appName);
        
        return savedApp;
    }

    /**
     * 禁用应用
     */
    public boolean disableApp(String appId) {
        Optional<AppCredentials> appOpt = appCredentialsRepository.findByAppId(appId);
        if (appOpt.isPresent()) {
            AppCredentials app = appOpt.get();
            app.setStatus(0);
            appCredentialsRepository.save(app);
            LOGGER.info("应用已禁用 - appId: {}", appId);
            return true;
        }
        return false;
    }

    /**
     * 启用应用
     */
    public boolean enableApp(String appId) {
        Optional<AppCredentials> appOpt = appCredentialsRepository.findByAppId(appId);
        if (appOpt.isPresent()) {
            AppCredentials app = appOpt.get();
            app.setStatus(1);
            appCredentialsRepository.save(app);
            LOGGER.info("应用已启用 - appId: {}", appId);
            return true;
        }
        return false;
    }

    /**
     * 检查appId是否存在
     */
    public boolean existsByAppId(String appId) {
        return appCredentialsRepository.existsByAppId(appId);
    }
}