<img width="1672" height="941" alt="9aa659ea-7965-4d3b-ae24-1754f9ea5a40" src="https://github.com/user-attachments/assets/6cb5f1e2-aa7a-4a4e-93f6-966c2600b38b" />

# Allay Auth

Allay Auth is a Paper/Folia Minecraft plugin that links Minecraft UUIDs to Discord accounts and requires Discord confirmation for logins.

## Features

- First join flow: Minecraft player gets a one-time `XXX-XXX` code, then links through Discord `/auth code:<code>`.
- Returning player flow: the Discord bot sends a DM with login details and buttons to confirm, trust the IP, or deny the login.
- Limbo/freeze mode blocks movement, chat, commands, damage, block changes, inventory interaction, portals, vehicles, item drop/pickup, and entity interaction until auth is complete.
- Folia-safe scheduler adapter with reflective Folia dispatch and Paper/Bukkit fallback.
- SQLite by default, with PostgreSQL and MySQL/MariaDB backends.
- Discord log-channel audit messages plus persistent `audit_logs`.
- Optional local MaxMind GeoIP country lookup.
- Optional bearer-token protected HTTP health endpoint.

## Requirements

- Java 21+ for Paper/Folia/Purpur 1.20+.
- Paper/Folia 1.21.x target, with Paper/Purpur 1.20+ compatibility.
- Maven for building.

The project uses Java 21 source/target. A separate Java 17 legacy build profile can be added later if you need one jar specifically compiled for old 1.16.1 server stacks.

## Build

```bash
mvn clean package
```

The shaded jar will be created at:

```text
target/AllayAuth-1.0.0-SNAPSHOT.jar
```

## Installation

1. Build the plugin jar.
2. Put the jar into your server `plugins/` folder.
3. Start the server once to generate `plugins/AllayAuth/config.yml`.
4. Stop the server and edit the config.
5. Set at minimum:
   - `discord.bot-token`
   - `discord.guild-id`
   - `discord.log-channel-id`
   - `discord.owner-ids`
   - `security.secret`
   - `messages.server-link`
6. Start the server again.

If the Discord bot token is empty or the bot cannot connect, the plugin stays enabled, but players are kicked on join because auth cannot be completed.

## Discord bot setup

Create a Discord application and bot in the Discord Developer Portal, copy the bot token, and invite it with permissions to:

- register slash commands
- send direct messages
- send messages in the configured log channel

Slash commands are registered automatically on bot ready. If `discord.guild-id` is configured, commands are registered to that guild for faster updates.

## MaxMind GeoIP

GeoIP uses a local MaxMind database and never sends player IPs to an external API during login.

1. Download `GeoLite2-Country.mmdb` from MaxMind.
2. Put it at `plugins/AllayAuth/GeoLite2-Country.mmdb`, or change `geoip.database-file`.
3. Keep:

```yaml
geoip:
  enabled: true
  provider: "maxmind-local"
```

If the `.mmdb` file is missing, GeoIP is disabled automatically and the plugin continues to run.

## Minecraft commands

| Command | Permission |
|---|---|
| `/allayauth reload` | `allayauth.reload` |
| `/allayauth status <player>` | `allayauth.info` |
| `/allayauth unlink <player>` | `allayauth.unlink` |
| `/allayauth force-link <player> <discordId>` | `allayauth.admin` |
| `/allayauth sessions <player>` | `allayauth.info` |
| `/allayauth revoke-session <player>` | `allayauth.moderator` |
| `/allayauth debug <player>` | `allayauth.admin` |

## Discord slash commands

| Command | Access |
|---|---|
| `/auth code:<code>` | everyone |
| `/unlink` | everyone |
| `/status` | everyone |
| `/dropauth discord:<@user>` | owner/mod |
| `/dropauth minecraft:<nick>` | owner/mod |
| `/authinfo discord:<@user>` | owner/mod |
| `/authinfo minecraft:<nick>` | owner/mod |
| `/authsessions minecraft:<nick>` | owner/mod |
| `/revokesession minecraft:<nick>` | owner/mod |
| `/config get/set/list/reload` | owner only |

Protected keys cannot be changed through Discord `/config`:

- `discord.bot-token`
- `storage.postgres.password`
- `storage.mysql.password`
- `security.secret`

## Storage

Default SQLite config:

```yaml
storage:
  type: "sqlite"
  sqlite-file: "plugins/AllayAuth/database.db"
```

Supported values: `sqlite`, `postgresql`, `postgres`, `mysql`, `mariadb`.

## Web API

Enable it with:

```yaml
web:
  enabled: true
  host: "127.0.0.1"
  port: 25526
  token: "CHANGE_ME"
```

Every request must include:

```text
Authorization: Bearer <web.token>
```

Endpoints:

- `GET /health`
- `GET /stats`
- `GET /players/pending`
- `GET /players/authenticated`

## Notes

- IPs are hashed in the database when `security.hash-ip-in-database: true`.
- Full IPs are shown only in Discord DM and admin log messages under spoiler formatting.
- Bypass through `allayauth.bypass` is always audited.
- Real player inventory is not cleared during limbo.
