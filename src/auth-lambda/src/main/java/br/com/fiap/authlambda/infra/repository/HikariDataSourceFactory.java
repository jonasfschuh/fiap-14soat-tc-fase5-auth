package br.com.fiap.authlambda.infra.repository;

import br.com.fiap.authlambda.config.DbSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Objects;

public final class HikariDataSourceFactory {

    private HikariDataSourceFactory() {
    }

    public static DataSource create(DbSettings settings) {
        Objects.requireNonNull(settings, "settings");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.url());
        config.setUsername(settings.user());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(Math.max(1, settings.poolSize()));
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5_000);
        config.setIdleTimeout(60_000);
        config.setMaxLifetime(1_800_000);
        config.setPoolName("auth-lambda-pool");
        config.setInitializationFailTimeout(-1);

        return new HikariDataSource(config);
    }
}

