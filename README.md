# PackForge

A Paper plugin (Minecraft 26.3) that turns plain PNG folders into a server resource pack, hosts it and sends it to
players on join. No JSON, no pixel math, no manual SHA-1 hashes. Works on any server: each server only sets its own
`namespace`.

- **Item models** from `items/*.png` (or Blockbench `.json` models) via the `item_model` component
- **Glyphs** for chat, scoreboard, tab list, titles and item names from `glyphs/*.png`
- **Custom menu backgrounds** from `guis/*.png`, aligned automatically behind the inventory title
- **Built-in web server** that hosts the pack, or use any external direct-download URL
- **API and MiniMessage tags** (`<glyph:coin>`, `<gui:shop>`, `<shift:-8>`) for your own plugins

Full documentation: [DOCS.md](DOCS.md) (German version: [DOCS.de.md](DOCS.de.md))

## Download

Get the latest `PackForge-*.jar` from the [Releases](../../releases) page and put it into your server's `plugins/` folder.

## Quick start

1. Start the server once. PackForge creates `plugins/PackForge/` with `config.yml` and the `assets/` folders.
2. Set `namespace` in `config.yml` (e.g. `myserver`).
3. Drop PNGs into `assets/items/`, `assets/glyphs/` or `assets/guis/`.
4. Run `/pack reload`. Online players receive the new pack right away.

```
plugins/PackForge/assets/
├─ items/icon/coin.png    -> item model  myserver:icon/coin
├─ glyphs/coin.png        -> <glyph:coin> in any MiniMessage text
├─ guis/shop.png          -> menu background "shop" (176 px wide)
├─ raw/                   -> copied into the pack as-is
└─ pack.png               -> pack icon
```

## Commands

Permission `packforge.admin` (ops have it by default).

| Command | Description |
| --- | --- |
| `/pack reload` | Reload `config.yml`, rebuild the pack and resend it if it changed |
| `/pack list` | List all items, glyphs and menus |
| `/pack give <item>` | Get a test item with the model |
| `/pack preview <gui>` | Open an empty inventory with the menu background |
| `/pack send [players]` | Send the pack again |

## Using the API

```kotlin
repositories { mavenLocal() }  // after "gradlew publishToMavenLocal" in this project
dependencies { compileOnly("me.mangokevin:packforge:1.0-SNAPSHOT") }
```

```yaml
# paper-plugin.yml of your plugin
dependencies:
  server:
    PackForge:
      load: BEFORE
      required: true
```

```java
PackApi pack = PackForge.api();
pack.applyModel(item, "icon/coin");
Component title = pack.guiTitle("shop", Component.text("Shop"));
Component line = MiniMessage.miniMessage().deserialize("<glyph:coin> 250", pack.tags());
```

## Hosting the pack

`host.public-url` is the address clients download from. `http://localhost:8163/pack.zip` only works on your own PC.
For real players, use your public IP or domain with the port forwarded, or upload `plugins/PackForge/output/pack.zip`
somewhere with a direct download link (for example as an asset of a GitHub release in a **public** repository), set
`host.enabled: false` and put that link into `host.public-url`.

## Building

Requires Java 25.

| Task | Description |
| --- | --- |
| `gradlew build` | Builds `build/libs/PackForge-<version>.jar` |
| `gradlew deploy` | Copies the jar into every folder listed in `deployDirs` (`gradle.properties`) |
| `gradlew publishToMavenLocal` | Publishes the API for other projects |
| `gradlew runServer` | Starts a test server in `run/` |

Pushing a tag like `v1.0.0` builds the plugin on GitHub Actions and publishes a release with the jar.
