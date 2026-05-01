package com.example.ecommerce.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 * 
 * @author system
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * 配置缓存管理器（使用本地内存缓存）
     * 生产环境建议使用Redis
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        // 定义缓存名称
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "analysisSummary",      // 分析汇总缓存
            "userSegmentation",    // 用户分层缓存
            "topCategories"        // 热门类别缓存
        ));
        return cacheManager;
    }
}

