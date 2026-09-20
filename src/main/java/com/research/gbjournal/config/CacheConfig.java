package com.research.gbjournal.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "current-issue",
                "all-issues",
                "issue-by-key",
                "articles",
                "article-types",
                "article-topics"
        );
    }
}
