package com.amalitech.module6.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.rds.RdsUtilities;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Connects to PostgreSQL through RDS Proxy using IAM database authentication.
 * The proxy requires IAM auth + TLS, so the task holds no database password:
 * each new pooled connection gets a fresh SigV4 token signed with the ECS
 * task role credentials.
 */
@Configuration
@ConditionalOnProperty(name = "app.db.iam-auth", havingValue = "true")
public class IamAuthDataSourceConfig {

    @Bean
    public DataSource dataSource(@Value("${app.db.host}") String host,
                                 @Value("${app.db.port}") int port,
                                 @Value("${app.db.name}") String database,
                                 @Value("${app.db.username}") String username) {
        RdsUtilities rdsUtilities = RdsUtilities.builder()
                .region(DefaultAwsRegionProviderChain.builder().build().getRegion())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        IamAuthPgDataSource pg = new IamAuthPgDataSource(rdsUtilities);
        pg.setServerNames(new String[]{host});
        pg.setPortNumbers(new int[]{port});
        pg.setDatabaseName(database);
        pg.setUser(username);
        pg.setSslMode("require");
        pg.setApplicationName("module-6-app");

        HikariConfig config = new HikariConfig();
        config.setPoolName("module-6-rds-proxy");
        config.setDataSource(pg);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        // IAM tokens are valid for 15 minutes and only checked at connect time.
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(14));
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));
        return new HikariDataSource(config);
    }
}
