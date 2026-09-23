# TOQUE Resource Pack

`TOQUE_Death_ResourcePack.zip` carries the `toque:death` sound the server plays when
a run ends. The server pushes it to players automatically; nobody installs it by hand.

## It is served straight from this path

`server.properties` points at the raw URL and pins the file by hash:

```properties
resource-pack=https://raw.githubusercontent.com/JosueBrenes/Death_Alert/main/resourcepack/TOQUE_Death_ResourcePack.zip
resource-pack-sha1=7b57fb56c92e528a81e001797febf04b72c2a8cc
```

So the two are coupled, and each breaks differently:

- **Move or rename the file** and the URL 404s. The client reports
  `Failed to download ... FileNotFoundException` and the death sound goes silent
  with `Unable to play unknown soundEvent: toque:death`. The hash still matches,
  because a hash is of the contents, not of the path.
- **Change the contents** and the hash no longer matches, so the client refuses the
  download even though the URL resolves.

Either change means editing `server.properties` to match. Rebuilding the zip is not
byte for byte reproducible, so a rebuild always needs a fresh
`resource-pack-sha1`; compute it with `sha1sum`.
