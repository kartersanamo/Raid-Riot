# Raid Riot

Spigot 1.8.8 faction raiding event plugin. Two teams compete in a dedicated arena; the first team to breach the other's base wins, or the team with greater wall penetration when time expires.

**Dependencies:** SaberFactions (required, via reflection). WorldEdit (optional, schematic paste).

## Event lifecycle

`IDLE → QUEUE_OPEN → QUEUE_LOCKED → [VOTING] → PREPARING → COUNTDOWN → ACTIVE → ENDING → RESTORING`

Voting is skipped when `fixed-match-settings.enabled` is true (base and kit taken from config). Admin can stop a session at any point.

## Queue

Two assignment modes, started by admin:

- **Random** — players join an open queue; teams are assigned when the queue locks at capacity or when the countdown expires.
- **Faction** — players must be in a faction; the first two factions to reach `players-per-team` members become the teams. Faction queue supports a higher cap (`max-faction-queue-players`).

Queue lock triggers base/kit resolution (vote or fixed settings), arena preparation, and participant qualification trimming.

## Voting

When enabled, participants vote via GUI for:

- **Base:** Easy, Medium, Hard (schematic), or Faction Base (copied from a faction's base claims in configured source worlds).
- **Kit:** Predefined kit (saved via admin or config fallback) or own inventory at match start.

## Arena preparation

- Bases are placed in a configured event world at a paste anchor with configurable separation.
- Schematic bases load from `plugins/RaidRiot/schematics/` (managed via `bases.yml` and admin commands).
- Faction bases copy chunk data cross-world from SaberFactions base claims.
- Memberless system factions (configurable tags, e.g. Yellow/Red) are created on enable and used to claim base territory during the event.
- World border is resized to fit both bases.
- Participant inventories and locations are snapshotted before the match; kits are applied at countdown end.

## Match rules

**Win conditions**

| Reason | Condition |
|--------|-----------|
| Breach | Attacker penetrates the enemy base past the wall plane (depth ≥ 1) |
| Depth | Match timer expires; team with higher recorded wall depth wins |
| Draw | Equal depth at timer expiry when `draw-on-equal-depth` is true |
| Admin stop | Manual termination |

**Breach detection** — Block breaks, explosions, and TNT attribution (dispenser/spawn tracking) feed penetration checks against each team's base bounds and wall region.

**Depth tracking** — Sampled on an interval during active play from participant block activity in enemy territory.

**Combat** — Virtual death: no item drops, title/subtitle, temporary spectator mode at team base, timed respawn with kit reapply. SaberFactions friendly-fire metadata is toggled for participants so cross-team PvP works within the same event faction.

**Building restrictions** — Non-participants cannot modify blocks inside base bounds during an active match. Participants cannot break/place in enemy faction claims. Patching on own cannon/base region requires no armor.

**In-match claiming** — `/f claim` (and aliases) by participants claims chunks for their event team faction instead of their personal faction.

## Spectators

Non-participants can spectate during an active match (`/rr` opens spectator GUI). Clicking player heads teleports to that player. Enter/leave restores a full state snapshot (location, inventory, gamemode).

## World restore

Block changes in the event world are tracked as deltas (non-air chunk snapshots). After a match or shutdown, terrain is restored asynchronously with configurable blocks/chunks per tick to avoid main-thread freezes.

## Player interface

- **Commands:** `/raidriot` (`/rr`) — open GUI, join, leave, status; admin subcommands for world setup, start/stop, schematic management, kit save, reload.
- **GUI:** 54-slot inventories for queue, voting, match status, and spectating (team-separated player heads, glass fill, live refresh during queue/vote/active phases).
- **Messages:** All player-facing text and GUI strings live in `config.yml` under `messages` and `gui`, loaded through `ConfigManager`.

## Configuration surface

Gameplay settings (team sizes, timers, kit, schematic offsets, faction tags, world-restore throttle, breach materials, etc.), messages, and GUI text are in a single `config.yml`. `ConfigManager` exposes typed getters for settings and path-based lookup/formatting for strings.

## Permissions

`raidriot.use` (default true), `raidriot.admin` and children for start, stop, reload, and arena management.
