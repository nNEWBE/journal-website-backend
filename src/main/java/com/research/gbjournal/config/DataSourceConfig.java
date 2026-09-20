package com.research.gbjournal.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username:}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("org.postgresql.Driver");

        String jdbcUrl = databaseUrl;
        String dbUser = username;
        String dbPass = password;

        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            throw new IllegalStateException(
                "DATABASE_URL is not configured! Please add 'DATABASE_URL' to your environment variables (e.g. from Supabase PostgreSQL or cloud provider)."
            );
        }

        // Support cloud database URL format (e.g. postgres://user:pass@host:port/db)
        if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
            try {
                URI dbUri = new URI(databaseUrl);
                String host = dbUri.getHost();
                int port = dbUri.getPort() == -1 ? 5432 : dbUri.getPort();
                String path = dbUri.getPath();
                String query = dbUri.getQuery();

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + (query != null ? "?" + query : "");

                if (dbUri.getUserInfo() != null && (dbUser == null || dbUser.isBlank())) {
                    String[] userInfo = dbUri.getUserInfo().split(":", 2);
                    dbUser = userInfo[0];
                    if (userInfo.length > 1 && (dbPass == null || dbPass.isBlank())) {
                        dbPass = userInfo[1];
                    }
                }
            } catch (Exception e) {
                // Fallback to prepending jdbc: if URI parsing fails
                if (!databaseUrl.startsWith("jdbc:")) {
                    jdbcUrl = "jdbc:" + databaseUrl;
                }
            }
        }

        hikariConfig.setJdbcUrl(jdbcUrl);
        if (dbUser != null && !dbUser.isBlank()) {
            hikariConfig.setUsername(dbUser);
        }
        if (dbPass != null && !dbPass.isBlank()) {
            hikariConfig.setPassword(dbPass);
        }

        // Optimized pool tuning for Supabase (resilient against high cross-continent latency)
        hikariConfig.setMaximumPoolSize(15);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(60000);
        hikariConfig.setInitializationFailTimeout(60000);
        hikariConfig.setKeepaliveTime(30000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setIdleTimeout(300000);

        hikariConfig.addDataSourceProperty("socketTimeout", "60");
        hikariConfig.addDataSourceProperty("connectTimeout", "30");
        hikariConfig.addDataSourceProperty("loginTimeout", "30");
        hikariConfig.addDataSourceProperty("tcpKeepAlive", "true");

        return new HikariDataSource(hikariConfig);
    }
}
