package dev.allayauth.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

public final class SQLiteStorage extends AbstractJdbcStorage {
    public SQLiteStorage(Plugin plugin, String sqliteFile, Executor executor) {
        super(createDataSource(plugin, sqliteFile), executor, plugin.getLogger(), Dialect.SQLITE);
    }

    private static HikariDataSource createDataSource(Plugin plugin, String sqliteFile) {
        File file = new File(sqliteFile);
        if (!file.isAbsolute()) {
            file = new File(plugin.getServer().getWorldContainer(), sqliteFile);
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            Logger.getLogger(SQLiteStorage.class.getName()).warning("Could not create SQLite directory: " + parent);
        }
        HikariConfig config = new HikariConfig();
        config.setPoolName("AllayAuth-SQLite");
        config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.addDataSourceProperty("foreign_keys", "true");
        return new HikariDataSource(config);
    }
}
