# PackForge Documentation

## Overview

PackForge is a Paper plugin for Minecraft 26.3 that builds a resource pack from plain PNG folders, hosts it itself and sends it to players. You don't write JSON, calculate pixel offsets or maintain a hash by hand.

How it works:

1. You put images into `plugins/PackForge/assets/` (items, glyphs, menu backgrounds).
2. `/pack reload` (or server start) builds `output/pack.zip` from them: models, fonts, `pack.mcmeta`, SHA-1 hash.
3. The built-in web server serves the zip at `host.public-url`.
4. Every player receives the pack on join. After a reload with changed files, all online players get the new pack immediately.
5. Your plugins use the content through the API (`PackForge.api()`) or through MiniMessage tags like `<glyph:coin>`.

PackForge is not tied to any project. Each server only sets its own `namespace`.

## Installation

Requires a Paper 26.3 server and Java 25.

**Download:** grab `PackForge-<version>.jar` from the [Releases](../../releases) page and put it into `plugins/`.

**Build from source:**

| Gradle command | What it does |
| --- | --- |
| `gradlew build` | Builds `build/libs/PackForge-<version>.jar` |
| `gradlew deploy` | Builds and copies the jar into every folder listed in `deployDirs` |
| `gradlew publishToMavenLocal` | Makes the API available to other projects (see API) |
| `gradlew runServer` | Starts a test server in `PackForge/run/` |

**Deploying to several servers:** `gradle.properties` lists the target folders, separated by `;`. Relative paths start at the PackForge folder.

```properties
deployDirs=../MyServer/run/plugins;../Lobby/run/plugins
```

Restart the server as usual after `gradlew deploy`.

**First start:** PackForge creates this in the server's plugin folder:

```
plugins/PackForge/
├─ config.yml           settings (see Configuration)
├─ chars.yml            assigned font characters, do not edit by hand
├─ assets/
│  ├─ items/            item textures and models
│  ├─ glyphs/           icons for text
│  ├─ guis/             menu backgrounds
│  └─ raw/              files copied into the pack as-is
└─ output/pack.zip      the finished pack
```

Then set at least `namespace` in `config.yml` and run `/pack reload`.

## Configuration

All values live in `plugins/PackForge/config.yml` and are applied with `/pack reload`, no restart needed. This includes port changes.

| Key | Default | Meaning |
| --- | --- | --- |
| `namespace` | `custom` | Prefix of all files and keys, e.g. `myserver` gives `myserver:icon/coin`. Pick a unique one per server. |
| `pack.description` | `Server Resources` | Text shown in the client's resource pack menu |
| `pack.min-format` / `pack.max-format` | `97` / `97` | Pack format of the Minecraft version (26.3 = 97). Update after a Minecraft update. |
| `host.enabled` | `true` | Start the built-in web server |
| `host.bind` | `0.0.0.0` | Network address of the web server. `0.0.0.0` means reachable on all network interfaces. |
| `host.port` | `8163` | Web server port. Several servers on one machine need different ports. |
| `host.public-url` | `http://localhost:8163/pack.zip` | Address players download from (see Hosting) |
| `send.on-join` | `true` | Send the pack automatically on join |
| `send.required` | `true` | Required pack: players who decline are disconnected by their own client |
| `send.prompt` | hint text | Text in the download dialog, MiniMessage format |
| `items.handheld-folders` | `[weapon]` | Folders under `items/` whose items are held like tools |
| `glyphs.height` / `glyphs.ascent` | `8` / `7` | Default size of all glyphs (8/7 = normal text height) |
| `glyphs.overrides.<name>` | `logo: 16/12` | Custom `height`/`ascent` for single glyphs |
| `gui.ascent` | `13` | Vertical position of menu backgrounds. 13 = top edge at the window border. |
| `gui.title-offset` | `8` | Where the inventory title starts horizontally (vanilla: 8 px) |
| `gui.overrides.<name>` | empty | Custom `height`, `ascent` or `rows` for single menus |

Override example:

```yaml
glyphs:
  overrides:
    logo:   { height: 32, ascent: 24 }
    star:   { height: 10, ascent: 8 }
gui:
  overrides:
    upgrade: { ascent: 12, rows: 3 }
```

## Assets

All content goes into `plugins/PackForge/assets/`. After every change, `/pack reload` is enough.

**Naming rules (all folders):** lowercase letters, digits and `_ - .` only, no spaces or special characters. Subfolders are allowed and become part of the name: `items/icon/coin.png` is called `icon/coin`. Invalid files are skipped with a console warning.

### Items (`items/`)

For menu icons, hotbar items, weapons and armor looks.

| File | Result |
| --- | --- |
| `items/icon/coin.png` | Flat item `<namespace>:icon/coin` (like paper or a diamond) |
| `items/weapon/frost_blade.png` | Same, but held diagonally like a sword (folder from `items.handheld-folders`) |
| `items/weapon/axe.json` | Custom 3D model, e.g. from Blockbench, as item `weapon/axe` |
| `items/weapon/_axe_tex.png` | Texture for a model only, gets no item of its own because of the `_` |
| `items/icon/coin.png.mcmeta` | Animation for `coin.png` (frames stacked vertically in the PNG) |

- Size: 16x16 px is standard, 32x32 or 64x64 work too.
- **3D models:** export from Blockbench as "Java Block/Item". Textures in the model must point to `<namespace>:item/<path>`, e.g. `myserver:item/weapon/_axe_tex`.
- The item's base material still decides its behavior (damage, stack size, edibility); the model only changes the look. `PAPER` is a good base for pure icons.

### Glyphs (`glyphs/`)

Small images that sit in text like letters: chat, scoreboard, tab list, item names, boss bar, titles.

- Default size is 8 px high to match the font. A 16x16 PNG is scaled down but looks sharper at high GUI scale.
- Larger glyphs (logo, banner) get their own values under `glyphs.overrides`. `height` is the rendered height, `ascent` is how far the image rises above the baseline. `ascent` must be less than or equal to `height`.
- Tall glyphs overlap the line below. In the tab list, add empty lines after them.
- Each glyph gets a fixed private-use character automatically, stored in `chars.yml`. It stays the same when you add or remove other files.

### Menu backgrounds (`guis/`)

An image drawn over the chest window. Items still render on top and stay clickable.

- Width **176 px**, height `17 + rows × 18 + 97` (6 rows = 222 px, 3 rows = 168 px).
- Best template: `assets/minecraft/textures/gui/container/generic_54.png` from the client jar (`.minecraft/versions/26.3/26.3.jar`, open it with 7-Zip). Slot positions must stay the same (18 px grid, first slot at x=7, y=17).
- The number of rows is derived from the image height. If that is wrong, set `gui.overrides.<name>.rows`.
- PackForge calculates the horizontal shift from the image width. Check the position with `/pack preview <name>`. If the image is off vertically, adjust `gui.ascent` or `gui.overrides.<name>.ascent`.
- Transparent areas show the vanilla background underneath. For invisible filler slots, use an item with a fully transparent texture instead of gray glass panes.

### Other files

- **`raw/`** is copied into the pack as-is, for everything PackForge doesn't generate itself: sounds, language files, vanilla texture overrides. Example: `raw/assets/minecraft/textures/block/stone.png`.
- **`pack.png`** directly in `assets/` is the pack icon (64x64 or 128x128).

## Commands and permissions

All commands require the permission `packforge.admin`. OPs have it by default.

| Command | What it does |
| --- | --- |
| `/pack reload` | Reloads `config.yml`, rebuilds the pack and sends it to all online players if anything changed. Reports the number of items, glyphs, menus and warnings. |
| `/pack list` | Lists all items, glyphs (with preview) and menus |
| `/pack give <item>` | Gives you a paper with the item model for testing |
| `/pack preview <gui>` | Opens an empty inventory with the menu background to check its position |
| `/pack send` | Sends the pack to you again |
| `/pack send <player>` | Sends it to other players, selectors like `@a` work too |

Tab completion suggests the existing names for `give` and `preview`.

## Hosting and delivery

The client downloads the pack from a URL. Which URL belongs in `host.public-url` depends on who plays:

| Situation | `host.public-url` | What to do |
| --- | --- | --- |
| Testing alone on your machine | `http://localhost:8163/pack.zip` | Nothing, this is the default |
| Friends on the same network | `http://<your-LAN-IP>:8163/pack.zip` | Find the LAN IP (`ipconfig` / `ip a`), allow the port in your firewall |
| Players from the internet | `http://<public-IP-or-domain>:8163/pack.zip` | Forward port 8163 (TCP) in your router, like the Minecraft port |
| Own web space or GitHub release | Direct download link of the zip | Set `host.enabled: false` and upload `output/pack.zip` after every change |

Important:

- The URL must be a **direct download**. Preview pages like Google Drive or Dropbox don't work.
- **GitHub releases:** an asset of a release in a public repository is a direct download, e.g. `https://github.com/<user>/<repo>/releases/download/<tag>/pack.zip`. Upload a new asset (or a new release) whenever the pack changes.
- **Several servers on one machine** need different `host.port` values, e.g. 8163 and 8164.
- **Caching:** PackForge appends `?v=<hash>` to the URL and sends the SHA-1 hash along. The client only downloads again when the content changed. Unchanged assets always produce the same hash, so no extra download.
- **Required pack:** with `send.required: true` the client disconnects itself when the player declines. Failed downloads are logged to the console with player name and reason.
- The pack is sent on join, i.e. after entering the world. The player briefly sees the pack loading screen.

## API for your own plugins

### Setup

1. Run `gradlew publishToMavenLocal` once in the PackForge folder. Repeat after changes to PackForge.
2. In the other project's `build.gradle.kts`:

   ```kotlin
   repositories { mavenLocal() }
   dependencies { compileOnly("me.mangokevin:packforge:1.0-SNAPSHOT") }
   ```
3. In its `paper-plugin.yml`, so PackForge loads first:

   ```yaml
   dependencies:
     server:
       PackForge:
         load: BEFORE
         required: true
   ```

### Methods

Everything goes through `PackApi pack = PackForge.api();`.

| Method | Returns | Purpose |
| --- | --- | --- |
| `glyph("coin")` | `Component` | Glyph for text, white and without shadow |
| `guiTitle("shop", title)` | `Component` | Complete inventory title with background and your text |
| `guiBackground("shop")` | `Component` | Background only, text after it sits at the normal title position |
| `guiRows("shop")` | `int` | Rows the image is drawn for |
| `applyModel(item, "icon/coin")` | `ItemStack` | Sets the item model on an existing item |
| `itemModel("icon/coin")` | `NamespacedKey` | Key for `ItemMeta#setItemModel` |
| `shift(-8)` | `Component` | Invisible spacing in pixels, negative moves left (up to ±1023) |
| `tags()` | `TagResolver` | MiniMessage tags (see below) |
| `hasGlyph` / `hasGui` / `hasItem` | `boolean` | Check whether a name exists |
| `glyphNames` / `guiNames` / `itemNames` | `Set<String>` | All existing names |

Unknown names don't throw. A warning is logged once, and a red `?` appears instead of the glyph (no background for menus).

### MiniMessage tags

If your plugin reads config texts with MiniMessage, pass `pack.tags()` as a resolver. Then any config can use:

| Tag | Result |
| --- | --- |
| `<glyph:coin>` | Glyph `coin` |
| `<gui:shop>` | Menu background `shop` |
| `<shift:-8>` | 8 px to the left |

### Examples

```java
PackApi pack = PackForge.api();

// Menu with a custom background
Inventory inv = Bukkit.createInventory(holder, pack.guiRows("shop") * 9,
        pack.guiTitle("shop", Component.text("Shop", NamedTextColor.DARK_GRAY)));

// Icon item
ItemStack coin = pack.applyModel(new ItemStack(Material.PAPER), "icon/coin");

// Text with a glyph
player.sendMessage(Component.text().append(pack.glyph("coin")).append(Component.text(" 250 Coins")));

// Config text with tags, e.g. for scoreboard or tab list
Component line = MiniMessage.miniMessage().deserialize("<glyph:coin> <gold><coins>",
        TagResolver.resolver(pack.tags(), Placeholder.unparsed("coins", "250")));
```

Tip: call the API every time you build a menu or text instead of storing components permanently. That way `/pack reload` changes apply immediately.

## Troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| File missing from `/pack list` | Invalid name (uppercase, spaces, special characters) or not a real PNG. The warning is in the console. |
| Purple-black item | Pack not loaded, or the name in `applyModel` doesn't match. `/pack give` tests the model directly. |
| Glyph shows as a box | Pack not loaded. Check in the client under Options → Resource Packs whether the server pack is active. |
| Red `?` instead of a glyph | Name doesn't exist. See the console warning; `/pack list` shows valid names. |
| Menu image too high or too low | Adjust `gui.ascent` or `gui.overrides.<name>.ascent`, `/pack reload`, `/pack preview` |
| Menu image shifted left or right | Image is not 176 px wide, or `gui.title-offset` doesn't match |
| Wrong inventory size in `/pack preview` | Set `gui.overrides.<name>.rows` |
| Download fails (console: `FAILED_DOWNLOAD`, `INVALID_URL`) | `host.public-url` isn't reachable from the player's side. `localhost` only works on your own machine (see Hosting). |
| Console: web server can't start | Port in use, often by a second server. Pick another `host.port`. |
| Client reports an incompatible pack | `pack.min-format`/`max-format` doesn't match the client version |
| Changes don't show up | Forgot `/pack reload`, or with external hosting the new zip wasn't uploaded |

If none of this helps: open `output/pack.zip` and check that the file is under `assets/<namespace>/...`. The client log (`.minecraft/logs/latest.log`) names missing or broken pack files.
