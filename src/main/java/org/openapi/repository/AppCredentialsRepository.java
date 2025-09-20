package org.openapi.repository;

import org.openapi.entity.AppCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 应用凭证Repository接口
 */
@Repository
public interface AppCredentialsRepository extends JpaRepository<AppCredentials, Long> {

    /**
     * 根据appId查找应用凭证
     */
    Optional<AppCredentials> findByAppId(String appId);

    /**
     * 根据appId和状态查找应用凭证
     */
    Optional<AppCredentials> findByAppIdAndStatus(String appId, Integer status);

    /**
     * 验证应用凭证
     */
    @Query("SELECT a FROM AppCredentials a WHERE a.appId = :appId AND a.appSecret = :appSecret AND a.status = 1")
    Optional<AppCredentials> validateCredentials(@Param("appId") String appId, @Param("appSecret") String appSecret);

    /**
     * 检查appId是否存在
     */
    boolean existsByAppId(String appId);
}