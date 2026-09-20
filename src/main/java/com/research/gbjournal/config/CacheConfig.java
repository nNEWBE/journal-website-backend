package com.research.gbjournal.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.List.of(
                "userDetails",
                "dashboard-stats",
                "admin-users",
                "current-issue",
                "all-issues",
                "issue-by-key",
                "articles",
                "article-types",
                "article-topics",
                "pageContent",
                "pageContentAdmin",
                "allPageContent",
                "navigation",
                "navigationAdmin",
                "editorial-board"
        ));
        // Allow dynamic cache creation for any other runtime caches
        cacheManager.setAllowNullValues(true);
        return cacheManager;
    }
}
