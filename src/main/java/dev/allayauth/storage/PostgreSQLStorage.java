package dev.allayauth.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.Executor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public final class PostgreSQLStorage extends AbstractJdbcStorage {
    public PostgreSQLStorage(Plugin plugin, ConfigurationSection section, Executor executor) {
        super(createDataSource(section), executor, plugin.getLogger(), Dialect.POSTGRESQL);
    }

    private static HikariDataSource createDataSource(ConfigurationSection section) {
        String host = section.getString("host", "127.0.0.1");
        int port = section.getInt("port", 5432);
        String database = section.getString("database", "allayauth");
        HikariConfig config = new HikariConfig();
        config.setPoolName("AllayAuth-PostgreSQL");
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        config.setUsername(section.getString("username", "allayauth"));
        config.setPassword(section.getString("password", ""));
        config.setMaximumPoolSize(section.getInt("pool-size", 5));
        return new HikariDataSource(config);
    }
}
