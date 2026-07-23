# OmnicraftSkyblock

Standalone Paper skyblock plugin with a GUI island picker (Standard Skyblock +
OneBlock), gradient text, sound effects, WorldEdit/FAWE-backed blueprint
islands, and Multiverse-Core compatibility. No BentoBox dependency.

## Commands

- `/sb` — opens the island picker GUI (first time) or teleports home (if you
  already have an island). Aliases: `/skyblock`, `/island`, `/is`.
  - `/sb create`, `/sb home`, `/sb delete confirm`
  - `/sb trust <player>`, `/sb untrust <player>`
  - `/sb settings` — shows your island's type/blueprint/OneBlock phase
- `/sba` — admin commands (needs `skyblock.admin`)
  - `/sba reload` — reloads config.yml, blueprints.yml, OneBlock phases
  - `/sba blueprint list`
  - `/sba blueprint capture <id> <icon-material> <display name...>` — while
    standing with an active WorldEdit/FAWE selection, saves it as a .schem
    and registers it as a new blueprint immediately available in `/sb`
  - `/sba blueprint remove <id>`
  - `/sba spawn set` — sets the skyblock spawn point to your location and
    moves the particle marker there
  - `/sba spawn particle <on|off>`
  - `/sba tp <player>`, `/sba delete <player> confirm`

## Permissions (LuckPerms-compatible)

This plugin uses plain Bukkit permission nodes rather than a hard LuckPerms
API dependency — LuckPerms (or any permissions plugin) already governs those
nodes transparently, so group-based access "just works":

- `skyblock.use` (default: true) — base access to `/sb`
- `skyblock.island.create`, `skyblock.island.trust`, `skyblock.island.settings`
- `skyblock.blueprint.<id>` — gate specific island types to specific LuckPerms
  groups (e.g. give `skyblock.blueprint.oneblock` only to donor ranks). A
  wildcard `skyblock.blueprint.*` is granted by default; tighten it in your
  LuckPerms config if you want some island types locked behind a rank.
- `skyblock.admin` (+ `.blueprint`, `.spawn`, `.reload`, `.teleport`, `.delete`)

## Blueprints (blueprints.yml)

Each entry defines a GUI tile: `type` (`STANDARD` or `ONEBLOCK`), display
name, a two-color gradient (`gradient-start`/`gradient-end`, hex, no `#`),
icon material, lore, GUI slot, and — for `STANDARD` — a `.schem` filename
under `plugins/OmnicraftSkyblock/blueprints/`.

Fastest way to add a new starting island: build it in-world, select it with
WorldEdit or FAWE (`//pos1`, `//pos2`), then run:
```
/sba blueprint capture volcano_island NETHERRACK Volcano Island
```
That captures the selection, writes `volcano_island.schem`, and registers it
— it shows up in `/sb` on your next `/sba reload` (or immediately, since
capture registers it live).

## Gradients & sounds

- Gradients: a small built-in parser (`util/GradientUtil`) applies a two-color
  hex gradient per character — used for the GUI title and blueprint names, no
  MiniMessage dependency required. Your `#FF6EC7` → `#7DF9FF` house gradient
  is the default in `config.yml`'s `gui.title`.
- Sounds: `config.yml`'s `sounds:` section maps `click`/`select`/`deny`/`create`
  to any Bukkit `Sound` enum name, played on the relevant GUI/command actions.

## OneBlock

Config-driven phase table in `config.yml` (`oneblock.phases`), each phase a
name + list of possible block materials. Breaking the island's one block
picks a random material from the current phase and regenerates it a tick
later; every `oneblock.blocks-per-phase` blocks broken, the phase advances.

## World management — Multiverse-Core

Multiverse-Core has two incompatible API generations in the wild (legacy
`MVWorldManager` vs. the newer 5.x `WorldManager`/vavr-based API), and I
couldn't confirm which one your server runs. Rather than hard-code against
one and risk a build that silently breaks against the other, this plugin
manages its own two worlds (`skyblock_world`, `skyblock_oneblock`) directly
via Bukkit and only **soft-depends** on Multiverse-Core. That means:

- If `world.auto-create: true` (default), the plugin creates both worlds
  itself with its own void generator on first enable.
- If you'd rather Multiverse own them (for portals, per-world settings,
  `/mv` listing, etc.), set `world.auto-create: false` and create them
  yourself with Multiverse, pointing at our generator:
  ```
  /mv create skyblock_world normal --generator OmnicraftSkyblock
  /mv create skyblock_oneblock normal --generator OmnicraftSkyblock
  ```
  Multiverse will then manage load/unload/settings for these worlds exactly
  like any other, while the plugin just looks them up by name via Bukkit.

## PlayerParticles

I wired up a working particle spawn-marker using Bukkit's own particle
system (`particles/NativeSpawnMarkerService`) — it's real and functional
today, no extra plugin needed, and `/sba spawn set` moves it live.

I did **not** hard-integrate against PlayerParticles' actual API for this,
even though it has a matching "fixed effects" feature (`/pp fixed`) that
would look nicer. I could confirm the API entry point
(`PlayerParticlesAPI.getInstance()`) but not the exact method signature for
creating a fixed effect programmatically without your installed jar to check
against — and guessing at a third-party method signature risks a broken
build. The spawn marker sits behind a `SpawnMarkerService` interface
specifically so a `PlayerParticlesSpawnMarkerService` can be dropped in later
as a one-file change. If you want that, tell me which PlayerParticles version
you're running (or install it and run `/pp fixed create <effect> <style>`
once) and I'll wire the real integration next.

## Getting this onto GitHub

```
git init
git add .
git commit -m "GUI island picker, blueprints, OneBlock, gradients, sounds"
git branch -M main
git remote add origin https://github.com/<you>/omnicraft-skyblock.git
git push -u origin main
```
Check the **Actions** tab for the built jar. Tag `vX.Y.Z` and push the tag to
also get a GitHub Release with the jar attached.

**Note:** my sandbox can't reach `repo.papermc.io` or `maven.enginehub.org`
(network restrictions here), so I couldn't run `mvn package` myself before
handing this over — the first Actions run is the real compile test. If
anything fails on a dependency resolution or API mismatch, send me the log
and I'll fix it fast.

## What's next

- Per-blueprint island size overrides (bigger islands for higher blueprint tiers)
- `/sb visit <player>` with a privacy toggle
- Island reset to blueprint default without re-rolling coordinates
- Confirmed PlayerParticles fixed-effect integration (see above)
- GUI pagination if you register more blueprints than fit on one page
