# Allay Auth

<p align="center">
  <img width="1672" height="941" alt="Allay Auth banner" src="https://github.com/user-attachments/assets/6cb5f1e2-aa7a-4a4e-93f6-966c2600b38b" />
</p>

<p align="center">
  Discord-based 2FA and account linking for Paper/Folia Minecraft servers.
</p>

<p align="center">
  <b>Paper</b> · <b>Folia</b> · <b>Discord</b> · <b>SQLite/PostgreSQL/MySQL</b>
</p>

---

## What is it?

**Allay Auth** links Minecraft players to Discord accounts and protects logins with Discord confirmation.

On first join, the player receives a one-time code in Minecraft.  
After linking through Discord, future logins require confirmation from the linked Discord account.

```text
Minecraft join → Discord confirmation → access granted
```

## Features

- Discord account linking with `/auth code:<code>`
- Login confirmation through Discord DM buttons
- IP trust sessions
- Deny suspicious logins
- Folia-safe scheduler abstraction
- Limbo/freeze mode before auth
- SQLite by default
- PostgreSQL and MySQL/MariaDB support
- Discord audit log channel
- Optional local MaxMind GeoIP
- Optional protected HTTP health API

## Requirements

| Runtime | Version |
|---|---|
| Java | 21+ |
| Server | Paper / Folia / Purpur 1.20+ |
| Target | Paper / Folia 1.21.x |
| Build tool | Maven |

> Legacy Java 17 / 1.16.1 support can be added later as a separate compatibility build.

## Build

```bash
mvn clean package
```

Output:

```text
target/AllayAuth-1.0.0-SNAPSHOT.jar
```

## Installation

1. Build the jar.
2. Put it into `plugins/`.
3. Start the server once.
4. Edit `plugins/AllayAuth/config.yml`.
5. Restart the server.

Minimum required config:

```yaml
discord:
  bot-token: "YOUR_TOKEN"
  guild-id: "YOUR_GUILD_ID"
  log-channel-id: "YOUR_LOG_CHANNEL_ID"
  owner-ids:
    - "YOUR_DISCORD_ID"

security:
  secret: "CHANGE_ME_TO_LONG_RANDOM_SECRET"

messages:
  server-link: "https://discord.gg/example"
```

If the Discord bot is not configured, players will be kicked because authentication cannot be completed.

## Discord setup

Create a Discord application and bot in the Discord Developer Portal.

The bot needs permissions to:

- register slash commands
- send direct messages
- send messages in the configured log channel

Slash commands are registered automatically when the bot starts.

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

## Discord commands

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

Protected config keys cannot be changed from Discord:

```text
discord.bot-token
storage.postgres.password
storage.mysql.password
security.secret
```

## Storage

Default:

```yaml
storage:
  type: "sqlite"
  sqlite-file: "plugins/AllayAuth/database.db"
```

Supported backends:

```text
sqlite
postgresql
postgres
mysql
mariadb
```

## Web API

Optional local API:

```yaml
web:
  enabled: true
  host: "127.0.0.1"
  port: 25526
  token: "CHANGE_ME"
```

Requests require:

```text
Authorization: Bearer <web.token>
```

Endpoints:

```text
GET /health
GET /stats
GET /players/pending
GET /players/authenticated
```

## GeoIP

Allay Auth can use a local MaxMind database.

```yaml
geoip:
  enabled: true
  provider: "maxmind-local"
  database-file: "plugins/AllayAuth/GeoLite2-Country.mmdb"
```

If the database file is missing, GeoIP is disabled automatically.

No external GeoIP API is called during login.

## Security notes

- Player IPs can be hashed in the database.
- Full IPs are only shown in Discord DM/admin logs.
- IPs in Discord messages are displayed under spoiler formatting.
- `allayauth.bypass` is always audited.
- Limbo does not clear the real player inventory.

## License

See [`LICENSE`](LICENSE).
