package dev.allayauth.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.Executor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public final class MySQLStorage extends AbstractJdbcStorage {
    public MySQLStorage(Plugin plugin, ConfigurationSection section, Executor executor) {
        super(createDataSource(section), executor, plugin.getLogger(), Dialect.MYSQL);
    }

    private static HikariDataSource createDataSource(ConfigurationSection section) {
        String host = section.getString("host", "127.0.0.1");
        int port = section.getInt("port", 3306);
        String database = section.getString("database", "allayauth");
        HikariConfig config = new HikariConfig();
        config.setPoolName("AllayAuth-MySQL");
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC&characterEncoding=utf8");
        config.setUsername(section.getString("username", "allayauth"));
        config.setPassword(section.getString("password", ""));
        config.setMaximumPoolSize(section.getInt("pool-size", 5));
        return new HikariDataSource(config);
    }
}
