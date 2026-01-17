# Religion

Paper 1.21.x plugin that adds a prayer system with a 30-second bossbar countdown,
movement interruption penalties, and persistent prayer points.

## Features
- /pray starts a 30-second prayer with a bossbar countdown
- Moving during prayer cancels it, smites the player, and blocks prayer for 1 Minecraft day
- Successful prayer grants +1 prayer point
- /prayer leaderboard (or /prayer lb) shows the top 5 players
- PlaceholderAPI support: %religion_prayer_points%

## Requirements
- Paper 1.21.x (built against 1.21.1 API)
- PlaceholderAPI (optional, for placeholders)
- A /smite command provided by another plugin (for example, EssentialsX)

## Build
```sh
mvn -q package
```

## Install
1. Copy `target/religion-1.0.0.jar` into your server `plugins/` folder.
2. (Optional) Install PlaceholderAPI if you want placeholders.
3. Ensure a `/smite` command is available.
4. Restart the server.

## Commands
```text
/pray
/prayer leaderboard
/prayer lb
```

## Data
Prayer points and cooldowns are stored in `plugins/Religion/data.yml`.

## Placeholders
```text
%religion_prayer_points%
```
# Religion
