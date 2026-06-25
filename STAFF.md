# Raid Riot — Staff Guide

Quick reference for running events and answering player questions.

---

## What is Raid Riot?

A timed 2-team raid event in a dedicated arena world. Each team gets a base. **First team to breach the enemy obsidian wall wins.** If time runs out, the team that got **deepest into the enemy base** wins (tie = draw, if enabled).

**Teams:** Yellow Team vs Red Team (configurable names).

**Match length:** 25 minutes by default (`match-duration-seconds` in config).

**After every event:** The arena world is reset to its pre-event state.

---

## How players join and play

1. Staff opens a queue (see commands below).
2. Players run **`/rr`** (or `/raidriot`) and click the **TNT item** to join the queue.
3. When the queue closes, teams are assigned and bases are pasted in the event world.
4. A short countdown runs, then players are teleported to their base with the event kit.
5. Teams cannon/raid the enemy wall. A **breach** means getting through the obsidian wall (breaking it, blowing it open, or entering through a hole and reaching the required depth inside).
6. **Death:** No item drops. 10-second respawn at your base with your kit restored.
7. **Patching rule:** Players must be **naked** (no armor) to place blocks on their own base/cannon area.

### Player commands

| Command | What it does |
|---------|----------------|
| `/rr` | Opens the Raid Riot menu (join, spectate, rejoin, etc.) |
| `/rr leave` | Leave the queue, stop spectating, or leave the match (confirm twice during a match) |
| `/rr rejoin` | Return to your base if you left mid-match but are still enrolled |
| `/rr queue leave` | Same as leave while in queue |
| `/rr status` | Shows queue, voting, or live match info |

### Spectating

If enabled, non-participants can use **`/rr`** during an active match and click the TNT to spectate. They cannot interact with the arena.

---

## Staff commands

All admin commands require **`raidriot.admin`** (OP by default). Sub-permissions apply where noted.

| Command | Permission | What it does |
|---------|------------|--------------|
| `/rr admin` | `raidriot.admin` | Opens the admin GUI |
| `/rr admin setup world <world>` | `raidriot.admin` | Sets the event world (must be loaded) |
| `/rr admin start random` | `raidriot.admin.start` | Opens queue — random teams |
| `/rr admin start faction` | `raidriot.admin.start` | Opens queue — faction teams |
| `/rr admin forcestart` | `raidriot.admin.start` | Starts with 2+ players in queue (even split) |
| `/rr admin stopqueue [reason]` | `raidriot.admin.stop` | Cancels an open queue |
| `/rr admin stop` | `raidriot.admin.stop` | Stops match; opens winner picker if teams exist |
| `/rr admin stop <a\|b\|draw\|none> [reason]` | `raidriot.admin.stop` | Ends match and declares winner (or no winner) |
| `/rr admin base list` | `raidriot.admin.arena` | Lists schematic files per difficulty |
| `/rr admin base set <easy\|medium\|hard> <file>` | `raidriot.admin.arena` | Assigns a schematic from `plugins/RaidRiot/schematics/` |
| `/rr admin base clear <easy\|medium\|hard>` | `raidriot.admin.arena` | Removes a schematic assignment |
| `/rr admin kit set` | `raidriot.admin` | Saves your inventory as the predefined event kit |
| `/rr admin reload` | `raidriot.admin.reload` | Reloads config, bases, and kit |
| `/rr admin status` | `raidriot.admin` | Short live status (phase, prep, teams, restore) |

**Aliases:** `/raidriot` works the same as `/rr`.

### Typical event flow (staff)

1. Ensure event world is set: `/rr admin setup world Riot`
2. Start queue: `/rr admin start random` (or `faction`)
3. Wait for queue to fill or use `/rr admin forcestart`
4. Event runs automatically from there
5. If needed: `/rr admin stop` or `/rr admin stop a` / `b` / `draw`

---

## Common player questions

**“How do I join?”**  
Run `/rr` when the queue is open and click the TNT, or use the queue menu it opens.

**“Why can’t I join?”**  
Queue may be closed, full, or (faction mode) they may not be in a faction.

**“I disconnected — can I come back?”**  
Yes, if still enrolled: `/rr rejoin` (or `/rr` → rejoin via the menu). Works during prep, countdown, and active match.

**“How do we win?”**  
Breach the enemy obsidian wall first, or have greater depth when the timer ends.

**“What counts as a breach?”**  
Breaking obsidian in the wall, blowing a hole with TNT, or entering through an opening and getting far enough inside the enemy base. Standing outside or touching the wall without a hole does **not** count.

**“Why can’t I place blocks?”**  
Likely the naked-patch rule (remove armor on your base/cannon), enemy territory, or they are not a participant.

**“Can I claim land during the event?”**  
Participants can use `/f claim` in the event world — it claims for their **event team** (Yellow/Red), not their personal faction.

**“The whole enemy team left — who wins?”**  
The remaining team wins by forfeit automatically.

---

## Troubleshooting

| Issue | What to check |
|-------|----------------|
| Event won’t start | Event world loaded? Schematic files set (`/rr admin base list`)? Enough players? |
| “Schematic not found” | File missing from `plugins/RaidRiot/schematics/` or wrong name in `/rr admin base set` |
| “Waiting for base generation” | Arena still pasting — check `/rr admin status`; avoid starting another event |
| World not resetting | `/rr admin status` — restore may still be running; wait before next event |
| Wrong kit | Admin saves kit with `/rr admin kit set`, or edit `predefined-kit` in config |

---

## Files staff may need

| Path | Purpose |
|------|---------|
| `plugins/RaidRiot/config.yml` | Timers, team size, kit, messages, event world name |
| `plugins/RaidRiot/bases.yml` | Schematic metadata cache |
| `plugins/RaidRiot/schematics/` | Base schematic files (`.schematic`) |
| `plugins/RaidRiot/kit.yml` | Saved predefined kit (from `/rr admin kit set`) |

**Dependencies:** SaberFactions (required), WorldEdit (required for schematic bases).

---

## Permissions summary

| Permission | Default | Purpose |
|------------|---------|---------|
| `raidriot.use` | All players | `/rr`, join, leave, status |
| `raidriot.admin` | OP | Admin GUI, setup, kit, status |
| `raidriot.admin.start` | OP | Start / force-start queue |
| `raidriot.admin.stop` | OP | Stop queue or match |
| `raidriot.admin.reload` | OP | Reload config |
| `raidriot.admin.arena` | OP | Base schematic management |
