package dev.allayauth.geoip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import dev.allayauth.config.PluginConfig;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.Plugin;

public final class GeoIPService implements AutoCloseable {
    private final Plugin plugin;
    private final Duration cacheDuration;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private DatabaseReader reader;

    public GeoIPService(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.cacheDuration = config.geoIpCacheDuration();
        if (!config.geoIpEnabled()) {
            return;
        }
        File database = new File(config.geoIpDatabaseFile());
        if (!database.isAbsolute()) {
            database = new File(plugin.getServer().getWorldContainer(), config.geoIpDatabaseFile());
        }
        if (!database.isFile()) {
            plugin.getLogger().warning("GeoIP database not found at " + database.getAbsolutePath() + ". GeoIP is disabled.");
            return;
        }
        try {
            this.reader = new DatabaseReader.Builder(database).build();
            plugin.getLogger().info("GeoIP enabled with " + database.getAbsolutePath());
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not open GeoIP database: " + ex.getMessage());
        }
    }

    public Optional<String> countryCode(String ipAddress) {
        if (reader == null || ipAddress == null || ipAddress.isBlank() || "unknown".equals(ipAddress)) {
            return Optional.empty();
        }
        CacheEntry cached = cache.get(ipAddress);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return Optional.ofNullable(cached.countryCode());
        }
        try {
            String country = reader.country(InetAddress.getByName(ipAddress)).getCountry().getIsoCode();
            cache.put(ipAddress, new CacheEntry(country, Instant.now().plus(cacheDuration)));
            return Optional.ofNullable(country);
        } catch (IOException | GeoIp2Exception ex) {
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ex) {
                plugin.getLogger().warning("Could not close GeoIP database: " + ex.getMessage());
            }
        }
    }

    private record CacheEntry(String countryCode, Instant expiresAt) {
    }
}
