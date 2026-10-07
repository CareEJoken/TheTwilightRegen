# The Twilight Regen

**The Twilight Regen** is an addon for [The Twilight Forest](https://www.curseforge.com/minecraft/mc-mods/the-twilight-forest) that lets defeated bosses come back — on your terms.

## Boss respawn countdown
Defeat a Twilight Forest boss and a floating countdown appears above its spawner (default **5 minutes**, configurable). When it ends, the spawner block is restored in place and the structure becomes **un-conquered** again — the magic map X disappears.

- Server-synced 3D text display (same look as the Final Castle "WIP" text)
- Resolves when the chunk is loaded — it never force-loads chunks
- If the spot is blocked by a player-placed block, it keeps retrying until clear

## Knock the countdown out
Left-click the countdown text to **cancel that respawn for good**: the spawner never returns and the structure stays conquered (map X kept). Can be turned off in the config, making the countdown uninterruptible.

## Death chest fix
In vanilla Twilight Forest, a death chest landing on an occupied spot climbs upward and can silently overwrite existing loot (worst with the Lich, whose spawner drifts up one block per kill). This addon replaces the placement logic: the chest always ends up at the intended spot, and any old container in the way is broken and its contents spilled safely onto the ground.

## Config
| Option | Default | Description |
|---|---|---|
| `bossRespawnEnabled` | `true` | Master switch for the respawn feature |
| `bossRespawnDelaySeconds` | `300` | Countdown length in seconds (1–86400) |
| `bossRespawnMarkerBreakable` | `true` | Whether the countdown text can be punched to cancel the respawn |

## Requirements
- Minecraft **1.21.1**
- NeoForge **21.1+**
- **The Twilight Forest 4.8+** (required)

## Credits
Built on [The Twilight Forest](https://www.curseforge.com/minecraft/mc-mods/the-twilight-forest) by Team Twilight (LGPLv3). This addon is licensed under MIT.
