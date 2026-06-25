# Raid Riot — Staff Guide

Quick reference for running events and answering player questions.

---

## What is Raid Riot?

A timed 2-team raid event in a dedicated arena world. Each team gets a base. **First team to breach the enemy obsidian wall wins.** If time runs out, the team that got **deepest into the enemy base** wins (tie = draw).

**Teams:** Yellow Team vs Red Team

**Match length:** 25 minutes

**After every event:** The arena world is reset to its pre-event state.

---

## How players join and play

1. Event starts on the event schedule or staff opens a queue (see commands below)
2. Players run `**/rr`** (or `/raidriot`) and click the **TNT item** to join the queue.
3. When the queue closes, teams are assigned and bases are pasted in the event world.
4. A short countdown runs, then players are teleported to their base with the event kit.
5. Teams cannon/raid the enemy wall. A **breach** means getting through the obsidian wall (breaking it, blowing it open, or entering through a hole and reaching the required depth inside).
6. **Death:** No item drops. 10-second respawn at your base with your kit restored.
7. **Patching rule:** Players must be **naked** (no armor) to place blocks on their own base/cannon area.

### Player commands


| Command           | What it does                                                                        |
| ----------------- | ----------------------------------------------------------------------------------- |
| `/rr`             | Opens the Raid Riot menu (join, spectate, rejoin, etc.)                             |
| `/rr leave`       | Leave the queue, stop spectating, or leave the match (confirm twice during a match) |
| `/rr rejoin`      | Return to your base if you left mid-match but are still enrolled                    |
| `/rr queue leave` | Same as leave while in queue                                                        |
| `/rr status`      | Shows queue, voting, or live match info                                             |


### Spectating

Non-participants can use `/rr` during an active match and click the TNT to spectate. They cannot interact with the arena.

---

## Staff commands

All admin commands require `**raidriot.admin`** (OP by default). Sub-permissions apply where noted.


| Command                                        | Permission              | What it does                                            |
| ---------------------------------------------- | ----------------------- | ------------------------------------------------------- |
| `/rr admin`                                    | `raidriot.admin`        | Opens the admin GUI                                     |
| `/rr admin setup world <world>`                | `raidriot.admin`        | Sets the event world (must be loaded)                   |
| `/rr admin start random`                       | `raidriot.admin.start`  | Opens queue — random teams                              |
| `/rr admin start faction`                      | `raidriot.admin.start`  | Opens queue — faction teams                             |
| `/rr admin forcestart`                         | `raidriot.admin.start`  | Starts with 2+ players in queue (even split)            |
| `/rr admin stopqueue [reason]`                 | `raidriot.admin.stop`   | Cancels an open queue                                   |
| `/rr admin stop`                               | `raidriot.admin.stop`   | Stops match; opens winner picker if teams exist         |
| `/rr admin stop <a|b|draw|none> [reason]`      | `raidriot.admin.stop`   | Ends match and declares winner (or no winner)           |
| `/rr admin base list`                          | `raidriot.admin.arena`  | Lists schematic files per difficulty                    |
| `/rr admin base set <easy|medium|hard> <file>` | `raidriot.admin.arena`  | Assigns a schematic from `plugins/RaidRiot/schematics/` |
| `/rr admin base clear <easy|medium|hard>`      | `raidriot.admin.arena`  | Removes a schematic assignment                          |
| `/rr admin kit set`                            | `raidriot.admin`        | Saves your inventory as the predefined event kit        |
| `/rr admin reload`                             | `raidriot.admin.reload` | Reloads config, bases, and kit                          |
| `/rr admin status`                             | `raidriot.admin`        | Short live status (phase, prep, teams, restore)         |


**Aliases:** `/raidriot` works the same as `/rr`.

---

## Permissions summary


| Permission              | Default     | Purpose                       |
| ----------------------- | ----------- | ----------------------------- |
| `raidriot.use`          | All players | `/rr`, join, leave, status    |
| `raidriot.admin`        | OP          | Admin GUI, setup, kit, status |
| `raidriot.admin.start`  | OP          | Start / force-start queue     |
| `raidriot.admin.stop`   | OP          | Stop queue or match           |
| `raidriot.admin.reload` | OP          | Reload config                 |
| `raidriot.admin.arena`  | OP          | Base schematic management     |


