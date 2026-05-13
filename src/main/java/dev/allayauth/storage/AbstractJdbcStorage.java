package dev.allayauth.storage;

import com.zaxxer.hikari.HikariDataSource;
import dev.allayauth.auth.AuthCode;
import dev.allayauth.auth.AuthSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class AbstractJdbcStorage implements StorageBackend {
    private final HikariDataSource dataSource;
    private final Executor executor;
    private final Logger logger;
    private final Dialect dialect;

    AbstractJdbcStorage(HikariDataSource dataSource, Executor executor, Logger logger, Dialect dialect) {
        this.dataSource = dataSource;
        this.executor = executor;
        this.logger = logger;
        this.dialect = dialect;
    }

    @Override
    public CompletableFuture<Void> init() {
        return run(() -> {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                for (String sql : dialect.schema()) {
                    statement.execute(sql);
                }
            }
        });
    }

    @Override
    public CompletableFuture<Optional<LinkedAccount>> findLinkedAccount(UUID minecraftUuid) {
        return supply(() -> queryLinked("SELECT * FROM linked_accounts WHERE minecraft_uuid = ?", minecraftUuid.toString()));
    }

    @Override
    public CompletableFuture<Optional<LinkedAccount>> findLinkedAccountByDiscord(String discordId) {
        return supply(() -> queryLinked("SELECT * FROM linked_accounts WHERE discord_id = ?", discordId));
    }

    @Override
    public CompletableFuture<Optional<LinkedAccount>> findLinkedAccountByName(String minecraftName) {
        return supply(() -> queryLinked("SELECT * FROM linked_accounts WHERE LOWER(last_minecraft_name) = LOWER(?)", minecraftName));
    }

    @Override
    public CompletableFuture<Void> linkAccount(UUID minecraftUuid, String minecraftName, String discordId, String discordName) {
        return run(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(dialect.upsertLinkedAccount())) {
                statement.setString(1, minecraftUuid.toString());
                statement.setString(2, minecraftName);
                statement.setString(3, discordId);
                statement.setString(4, discordName);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> unlinkAccount(UUID minecraftUuid) {
        return supply(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM linked_accounts WHERE minecraft_uuid = ?")) {
                statement.setString(1, minecraftUuid.toString());
                return statement.executeUpdate() > 0;
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateLastLogin(UUID minecraftUuid) {
        return run(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE linked_accounts SET last_login_at = ? WHERE minecraft_uuid = ?")) {
                statement.setTimestamp(1, Timestamp.from(Instant.now()));
                statement.setString(2, minecraftUuid.toString());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> createPendingCode(AuthCode code) {
        return supply(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO pending_auth_codes(code, minecraft_uuid, player_ip, created_at, expires_at, used)
                         VALUES (?, ?, ?, ?, ?, ?)
                         """)) {
                statement.setString(1, code.code());
                statement.setString(2, code.minecraftUuid().toString());
                statement.setString(3, code.playerIpHash());
                statement.setTimestamp(4, Timestamp.from(code.createdAt()));
                statement.setTimestamp(5, Timestamp.from(code.expiresAt()));
                statement.setBoolean(6, code.used());
                statement.executeUpdate();
                return true;
            } catch (SQLException ex) {
                if (isUniqueViolation(ex)) {
                    return false;
                }
                throw new StorageRuntimeException(ex);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<AuthCode>> findPendingCode(String code) {
        return supply(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM pending_auth_codes WHERE code = ? AND used = ?")) {
                statement.setString(1, code);
                statement.setBoolean(2, false);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(readCode(rs));
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> deletePendingCode(String code) {
        return run(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM pending_auth_codes WHERE code = ?")) {
                statement.setString(1, code);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deletePendingCodes(UUID minecraftUuid) {
        return run(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM pending_auth_codes WHERE minecraft_uuid = ?")) {
                statement.setString(1, minecraftUuid.toString());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<AuthSession>> findValidSession(UUID minecraftUuid, String ipHash) {
        return supply(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT * FROM login_sessions
                         WHERE minecraft_uuid = ? AND ip_hash = ? AND revoked = ? AND expires_at > ?
                         ORDER BY expires_at DESC
                         LIMIT 1
                         """)) {
                statement.setString(1, minecraftUuid.toString());
                statement.setString(2, ipHash);
                statement.setBoolean(3, false);
                statement.setTimestamp(4, Timestamp.from(Instant.now()));
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? Optional.of(readSession(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> createSession(UUID minecraftUuid, String ipHash, String countryCode, Instant expiresAt) {
        return run(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO login_sessions(minecraft_uuid, ip_hash, country_code, created_at, expires_at, revoked)
                         VALUES (?, ?, ?, ?, ?, ?)
                         """)) {
                statement.setString(1, minecraftUuid.toString());
                statement.setString(2, ipHash);
                statement.setString(3, countryCode);
                statement.setTimestamp(4, Timestamp.from(Instant.now()));
                statement.setTimestamp(5, Timestamp.from(expiresAt));
                statement.setBoolean(6, false);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<List<AuthSession>> listSessions(UUID minecraftUuid) {
        return supply(() -> {
            List<AuthSession> sessions = new ArrayList<>();
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM login_sessions WHERE minecraft_uuid = ? ORDER BY expires_at DESC")) {
                statement.setString(1, minecraftUuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        sessions.add(readSession(rs));
                    }
                }
            }
            return sessions;
        });
    }

    @Override
    public CompletableFuture<Integer> revokeSessions(UUID minecraftUuid, String ipHash) {
        return supply(() -> {
            String sql = ipHash == null
                    ? "UPDATE login_sessions SET revoked = ? WHERE minecraft_uuid = ? AND revoked = ?"
                    : "UPDATE login_sessions SET revoked = ? WHERE minecraft_uuid = ? AND ip_hash = ? AND revoked = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBoolean(1, true);
                statement.setString(2, minecraftUuid.toString());
                if (ipHash == null) {
                    statement.setBoolean(3, false);
                } else {
                    statement.setString(3, ipHash);
                    statement.setBoolean(4, false);
                }
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> audit(String eventType, UUID minecraftUuid, String discordId, String ipHash, String details) {
        return run(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO audit_logs(event_type, minecraft_uuid, discord_id, ip_hash, details, created_at)
                         VALUES (?, ?, ?, ?, ?, ?)
                         """)) {
                statement.setString(1, eventType);
                statement.setString(2, minecraftUuid == null ? null : minecraftUuid.toString());
                statement.setString(3, discordId);
                statement.setString(4, ipHash);
                statement.setString(5, details);
                statement.setTimestamp(6, Timestamp.from(Instant.now()));
                statement.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Long> countLinkedAccounts() {
        return count("SELECT COUNT(*) FROM linked_accounts");
    }

    @Override
    public CompletableFuture<Long> countPendingAuth() {
        return count("SELECT COUNT(*) FROM pending_auth_codes WHERE used = ?");
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private CompletableFuture<Long> count(String sql) {
        return supply(() -> {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                if (sql.contains("?")) {
                    statement.setBoolean(1, false);
                }
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    private Optional<LinkedAccount> queryLinked(String sql, String value) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(readLinked(rs));
            }
        } catch (SQLException ex) {
            throw new StorageRuntimeException(ex);
        }
    }

    private LinkedAccount readLinked(ResultSet rs) throws SQLException {
        return new LinkedAccount(
                UUID.fromString(rs.getString("minecraft_uuid")),
                rs.getString("last_minecraft_name"),
                rs.getString("discord_id"),
                rs.getString("discord_name"),
                instant(rs.getTimestamp("linked_at")),
                instant(rs.getTimestamp("last_login_at"))
        );
    }

    private AuthCode readCode(ResultSet rs) throws SQLException {
        return new AuthCode(
                rs.getString("code"),
                UUID.fromString(rs.getString("minecraft_uuid")),
                rs.getString("player_ip"),
                instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("expires_at")),
                rs.getBoolean("used")
        );
    }

    private AuthSession readSession(ResultSet rs) throws SQLException {
        return new AuthSession(
                rs.getLong("id"),
                UUID.fromString(rs.getString("minecraft_uuid")),
                rs.getString("ip_hash"),
                rs.getString("country_code"),
                instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("expires_at")),
                rs.getBoolean("revoked")
        );
    }

    private Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private CompletableFuture<Void> run(SqlRunnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Database operation failed.", ex);
                throw new StorageRuntimeException(ex);
            }
        }, executor);
    }

    private <T> CompletableFuture<T> supply(SqlSupplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Database operation failed.", ex);
                throw new StorageRuntimeException(ex);
            } catch (StorageRuntimeException ex) {
                logger.log(Level.SEVERE, "Database operation failed.", ex.getCause() == null ? ex : ex.getCause());
                throw ex;
            }
        }, executor);
    }

    private boolean isUniqueViolation(SQLException ex) {
        String state = ex.getSQLState();
        int code = ex.getErrorCode();
        return "23505".equals(state) || code == 1062 || code == 19 || code == 2067;
    }

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    enum Dialect {
        SQLITE {
            @Override
            List<String> schema() {
                return List.of(
                        linkedAccounts("TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "TIMESTAMP"),
                        pendingCodes("INTEGER PRIMARY KEY AUTOINCREMENT", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "TIMESTAMP NOT NULL"),
                        loginSessions("INTEGER PRIMARY KEY AUTOINCREMENT", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "TIMESTAMP NOT NULL"),
                        auditLogs("INTEGER PRIMARY KEY AUTOINCREMENT", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
                );
            }

            @Override
            String upsertLinkedAccount() {
                return """
                        INSERT INTO linked_accounts(minecraft_uuid, last_minecraft_name, discord_id, discord_name, linked_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                        ON CONFLICT(minecraft_uuid) DO UPDATE SET
                          last_minecraft_name = excluded.last_minecraft_name,
                          discord_id = excluded.discord_id,
                          discord_name = excluded.discord_name
                        """;
            }
        },
        POSTGRESQL {
            @Override
            List<String> schema() {
                return List.of(
                        linkedAccounts("TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "TIMESTAMP"),
                        pendingCodes("SERIAL PRIMARY KEY", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "TIMESTAMP NOT NULL"),
                        loginSessions("SERIAL PRIMARY KEY", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "TIMESTAMP NOT NULL"),
                        auditLogs("SERIAL PRIMARY KEY", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
                );
            }

            @Override
            String upsertLinkedAccount() {
                return """
                        INSERT INTO linked_accounts(minecraft_uuid, last_minecraft_name, discord_id, discord_name, linked_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                        ON CONFLICT(minecraft_uuid) DO UPDATE SET
                          last_minecraft_name = excluded.last_minecraft_name,
                          discord_id = excluded.discord_id,
                          discord_name = excluded.discord_name
                        """;
            }
        },
        MYSQL {
            @Override
            List<String> schema() {
                return List.of(
                        linkedAccounts("TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "TIMESTAMP NULL"),
                        pendingCodes("BIGINT PRIMARY KEY AUTO_INCREMENT", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "TIMESTAMP NOT NULL"),
                        loginSessions("BIGINT PRIMARY KEY AUTO_INCREMENT", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "TIMESTAMP NOT NULL"),
                        auditLogs("BIGINT PRIMARY KEY AUTO_INCREMENT", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
                );
            }

            @Override
            String upsertLinkedAccount() {
                return """
                        INSERT INTO linked_accounts(minecraft_uuid, last_minecraft_name, discord_id, discord_name, linked_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                        ON DUPLICATE KEY UPDATE
                          last_minecraft_name = VALUES(last_minecraft_name),
                          discord_id = VALUES(discord_id),
                          discord_name = VALUES(discord_name)
                        """;
            }
        };

        abstract List<String> schema();

        abstract String upsertLinkedAccount();

        static String linkedAccounts(String linkedAtType, String lastLoginType) {
            return """
                    CREATE TABLE IF NOT EXISTS linked_accounts (
                        minecraft_uuid      VARCHAR(36) PRIMARY KEY,
                        last_minecraft_name VARCHAR(16) NOT NULL,
                        discord_id          VARCHAR(20) NOT NULL UNIQUE,
                        discord_name        VARCHAR(64),
                        linked_at           %s,
                        last_login_at       %s
                    )
                    """.formatted(linkedAtType, lastLoginType);
        }

        static String pendingCodes(String idType, String createdAtType, String expiresAtType) {
            return """
                    CREATE TABLE IF NOT EXISTS pending_auth_codes (
                        id              %s,
                        code            VARCHAR(7) NOT NULL UNIQUE,
                        minecraft_uuid  VARCHAR(36) NOT NULL,
                        player_ip       VARCHAR(128),
                        created_at      %s,
                        expires_at      %s,
                        used            BOOLEAN DEFAULT FALSE
                    )
                    """.formatted(idType, createdAtType, expiresAtType);
        }

        static String loginSessions(String idType, String createdAtType, String expiresAtType) {
            return """
                    CREATE TABLE IF NOT EXISTS login_sessions (
                        id              %s,
                        minecraft_uuid  VARCHAR(36) NOT NULL,
                        ip_hash         VARCHAR(128) NOT NULL,
                        country_code    VARCHAR(2),
                        created_at      %s,
                        expires_at      %s,
                        revoked         BOOLEAN DEFAULT FALSE
                    )
                    """.formatted(idType, createdAtType, expiresAtType);
        }

        static String auditLogs(String idType, String createdAtType) {
            return """
                    CREATE TABLE IF NOT EXISTS audit_logs (
                        id              %s,
                        event_type      VARCHAR(64) NOT NULL,
                        minecraft_uuid  VARCHAR(36),
                        discord_id      VARCHAR(20),
                        ip_hash         VARCHAR(128),
                        details         TEXT,
                        created_at      %s
                    )
                    """.formatted(idType, createdAtType);
        }
    }

    private static final class StorageRuntimeException extends RuntimeException {
        private StorageRuntimeException(Throwable cause) {
            super(cause);
        }
    }
}
