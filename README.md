# Josh's Blocks: Backrooms

> Place the trigger block. The lights flicker. You're somewhere else.

An original, **free-forever** backrooms dimension for Minecraft Java — part of the
[Josh's Blocks](https://github.com/Josh-Blocks) portfolio of open-source mods made for kids to enjoy. No
paywalls, no Marketplace, no monetisation.

This is a clean-room original: our own code, our own art, our own name. It is
*not* a copy of any other backrooms mod.

## What it does

> ⚠️ **In development** — feature-complete, art is placeholder. Every system below works; the textures, sounds and fog are next.

- [x] **Blocks:** mono-yellow wallpaper, damp carpet, ceiling tiles, humming fluorescent lights, and the trigger block (in a "Josh's Blocks: Backrooms" creative tab).
- [x] **The dimension:** place & right-click the trigger block to drop into an endless, procedurally generated yellow maze of rooms and doorways.
- [x] **Escape:** a 2-minute boss-bar timer counts down — last it out (or right-click another trigger block) and you're returned to exactly where you entered. *(Implemented; the return-point survives the crossing by design — final confirmation is a quick in-game playtest.)*
- [x] **The Lurker:** an original eyeless thing creeps in and hunts you through the corridors.
- [ ] **Polish (next):** original textures, ambient hum, eerie fog, a custom Lurker model.

## Install (once released)

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft **1.21.11**.
2. Drop [Fabric API](https://modrinth.com/mod/fabric-api) (`0.141.4+1.21.11` or newer) into your `mods/` folder.
3. Drop this mod's jar into `mods/`.

## Build from source

Requires **JDK 21**. From this folder:

```bash
./gradlew build        # produces the distributable jar in build/libs/
./gradlew runClient    # launches a dev Minecraft client to test
```

The jar in `build/libs/` *without* the `-sources` suffix is the one you distribute.

## Versions

| Component | Version |
| --- | --- |
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.141.4+1.21.11 |
| Yarn mappings | 1.21.11+build.6 |
| Loom | 1.17.12 |

## Licence

[MIT](./LICENSE) © 2026 Josh's Blocks. Attributions, if any, live in [CREDITS.md](./CREDITS.md).
