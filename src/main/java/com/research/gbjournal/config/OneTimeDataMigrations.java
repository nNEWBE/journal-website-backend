package com.research.gbjournal.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Data fixes that must run exactly once per database. Each applied key is recorded in
 * {@code app_data_migrations}, so restarts and redeploys never re-run it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OneTimeDataMigrations {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final CacheManager cacheManager;

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS app_data_migrations ("
                + "migration_key VARCHAR(120) PRIMARY KEY, "
                + "applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

        // Article view counts held placeholder numbers; real per-visit counting starts from zero.
        apply("2026-11-02-reset-article-views", () -> {
            int updated = jdbcTemplate.update("UPDATE articles SET views = 0");
            log.info("Reset view counts to 0 for {} articles.", updated);
        });
    }

    private void apply(String key, Runnable migration) {
        Integer done = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_data_migrations WHERE migration_key = ?", Integer.class, key);
        if (done != null && done > 0) return;
        transactionTemplate.executeWithoutResult(status -> {
            migration.run();
            jdbcTemplate.update("INSERT INTO app_data_migrations (migration_key) VALUES (?)", key);
        });
        var articles = cacheManager.getCache("articles");
        if (articles != null) articles.clear();
        log.info("Applied one-time data migration {}", key);
    }
}
