# TOQUE Server Banner

Client-side Fabric mod for Minecraft 1.21.1 that replaces the vanilla rendering of the TOQUE server entry in the Multiplayer screen with a custom animated banner.

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.19.5+
- Fabric API 0.116.17+1.21.1
- Java 21

## Client only

This mod is client-side. It does not need to be installed on the Minecraft server.

Players who want to see the custom TOQUE banner install the jar in their client `mods` folder.

## Features

- Custom TOQUE red/black server banner.
- 76px server rows so the banner has room to breathe.
- Animated rotating messages.
- Live player count from the server status response.
- Live ping bars.
- Hover glow/border.
- Reads TRY # and DÍA from the server MOTD when those values are present.
- Only modifies servers whose name/address contains `toque` or the TOQUE Playit address.
- Other servers keep their normal Minecraft appearance.

## Build

Use Java 21 and Gradle 8.10.2:

```text
gradle build
```

The jar is generated in `build/libs/`.
