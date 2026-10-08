# PackForge Dokumentation

## Überblick

PackForge ist ein Paper-Plugin für Minecraft 26.3, das aus einfachen PNG-Ordnern automatisch ein Resourcepack baut, es selbst hostet und an die Spieler schickt. Du schreibst kein JSON, rechnest keine Pixel-Abstände aus und musst keinen Hash von Hand pflegen.

So läuft es ab:

1. Du legst Bilder in `plugins/PackForge/assets/` ab (Items, Glyphen, Menü-Hintergründe).
2. `/pack reload` (oder der Serverstart) baut daraus `output/pack.zip`: Modelle, Fonts, `pack.mcmeta`, SHA-1-Hash.
3. Der eingebaute Webserver stellt die Zip unter `host.public-url` bereit.
4. Jeder Spieler bekommt das Pack beim Join. Nach einem Reload mit geänderten Dateien bekommen es alle Online-Spieler sofort neu.
5. Deine Plugins benutzen die Inhalte über die API (`PackForge.api()`) oder über MiniMessage-Tags wie `<glyph:coin>`.

PackForge ist unabhängig von einem bestimmten Projekt. Pro Server stellst du nur einen eigenen `namespace` ein.

## Installation

Das Projekt liegt unter `F:\dev-workspace\minecraft-workspace\PackForge`. Voraussetzung ist Java 25, wie bei Surwave.

| Gradle-Befehl | Was er macht |
| --- | --- |
| `gradlew build` | Baut `build/libs/PackForge-1.0-SNAPSHOT.jar` |
| `gradlew deploy` | Baut und kopiert die Jar in alle Ordner aus `deployDirs` |
| `gradlew publishToMavenLocal` | Stellt die API für andere Projekte bereit (siehe API) |
| `gradlew runServer` | Startet einen eigenen Testserver unter `PackForge/run/` |

**Auf mehrere Server verteilen:** In `PackForge/gradle.properties` stehen die Ziel-Ordner, getrennt mit `;`. Relative Pfade gehen vom PackForge-Ordner aus.

```properties
deployDirs=../Surwave/run/plugins;../Lobby/run/plugins
```

Nach `gradlew deploy` startest du den jeweiligen Server wie gewohnt neu.

**Erster Start:** PackForge legt im Plugin-Ordner des Servers an:

```
plugins/PackForge/
├─ config.yml           Einstellungen (siehe Konfiguration)
├─ chars.yml            vergebene Font-Zeichen, nicht von Hand ändern
├─ assets/
│  ├─ items/            Item-Texturen und Modelle
│  ├─ glyphs/           Icons für Texte
│  ├─ guis/             Menü-Hintergründe
│  └─ raw/              Dateien, die 1:1 ins Pack kommen
└─ output/pack.zip      das fertige Pack
```

Danach stellst du in der `config.yml` mindestens `namespace` ein und führst `/pack reload` aus.

## Konfiguration

Alle Werte stehen in `plugins/PackForge/config.yml` und werden mit `/pack reload` übernommen, ohne Neustart. Das gilt auch für Port-Änderungen.

| Schlüssel | Standard | Bedeutung |
| --- | --- | --- |
| `namespace` | `custom` | Präfix aller Dateien und Keys, z.B. `surwave` ergibt `surwave:icon/coin`. Pro Server eindeutig wählen. |
| `pack.description` | `Server Resources` | Text im Resourcepack-Menü des Clients |
| `pack.min-format` / `pack.max-format` | `97` / `97` | Pack-Format der Minecraft-Version (26.3 = 97). Nach einem Minecraft-Update anpassen. |
| `host.enabled` | `true` | Eingebauten Webserver starten |
| `host.bind` | `0.0.0.0` | Netzwerk-Adresse des Webservers. `0.0.0.0` heißt: auf allen Netzwerkkarten erreichbar. |
| `host.port` | `8163` | Port des Webservers. Mehrere Server auf einem PC brauchen verschiedene Ports. |
| `host.public-url` | `http://localhost:8163/pack.zip` | Adresse, von der die Spieler laden (siehe Hosting) |
| `send.on-join` | `true` | Pack automatisch beim Join schicken |
| `send.required` | `true` | Pflicht-Pack: Wer ablehnt, wird vom eigenen Client getrennt |
| `send.prompt` | Hinweistext | Text im Download-Dialog, MiniMessage-Format |
| `items.handheld-folders` | `[weapon]` | Ordner unter `items/`, deren Items wie Werkzeuge gehalten werden |
| `glyphs.height` / `glyphs.ascent` | `8` / `7` | Standardgröße aller Glyphen (8/7 = normale Schrifthöhe) |
| `glyphs.overrides.<name>` | `logo: 16/12` | Eigene `height`/`ascent` für einzelne Glyphen |
| `gui.ascent` | `13` | Vertikale Lage der Menü-Hintergründe. 13 = Oberkante am Fensterrand. |
| `gui.title-offset` | `8` | Wo der Inventartitel horizontal beginnt (Vanilla: 8 px) |
| `gui.overrides.<name>` | leer | Eigene `height`, `ascent` oder `rows` für einzelne Menüs |

Beispiel für Overrides:

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

Alle Inhalte kommen nach `plugins/PackForge/assets/`. Nach jeder Änderung reicht `/pack reload`.

**Namensregeln (für alle Ordner):** nur Kleinbuchstaben, Ziffern und `_ - .`, keine Leerzeichen oder Umlaute. Unterordner sind erlaubt und werden Teil des Namens: `items/icon/coin.png` heißt `icon/coin`. Ungültige Dateien werden mit einer Warnung in der Konsole übersprungen.

### Items (`items/`)

Für Icons in Menüs, Hotbar-Items, Waffen und Rüstungs-Optik.

| Datei | Ergebnis |
| --- | --- |
| `items/icon/coin.png` | Flaches Item `<namespace>:icon/coin` (wie Papier oder Diamant) |
| `items/weapon/frost_blade.png` | Wie oben, aber diagonal in der Hand wie ein Schwert (Ordner aus `items.handheld-folders`) |
| `items/weapon/axe.json` | Eigenes 3D-Modell, z.B. aus Blockbench, als Item `weapon/axe` |
| `items/weapon/_axe_tex.png` | Nur Textur für ein Modell, bekommt wegen `_` kein eigenes Item |
| `items/icon/coin.png.mcmeta` | Animation für `coin.png` (Frames untereinander in der PNG) |

- Größe: 16x16 px ist Standard, 32x32 oder 64x64 gehen auch.
- **3D-Modelle:** In Blockbench als "Java Block/Item" exportieren. Die Texturen im Modell müssen auf `<namespace>:item/<pfad>` zeigen, z.B. `surwave:item/weapon/_axe_tex`.
- Das Basis-Material des Items bestimmt weiter das Verhalten (Schaden, Stackgröße, Essbarkeit), das Modell nur das Aussehen. Für reine Icons eignet sich `PAPER`.

### Glyphen (`glyphs/`)

Kleine Bilder, die wie Buchstaben in Texten stehen: Chat, Scoreboard, Tablist, Item-Namen, Bossbar, Titel.

- Standardgröße 8 px hoch, passend zur Schrift. Ein 16x16-PNG wird verkleinert, sieht bei hoher GUI-Skalierung aber schärfer aus.
- Größere Glyphen (Logo, Banner) bekommen eigene Werte unter `glyphs.overrides`. `height` ist die Darstellungshöhe, `ascent` wie weit die Grafik über die Textlinie ragt. `ascent` muss kleiner oder gleich `height` sein.
- Hohe Glyphen überlappen die Zeile darunter. In der Tablist kommen deshalb leere Zeilen danach.
- Jede Glyphe bekommt automatisch ein festes Sonderzeichen, gespeichert in `chars.yml`. Es bleibt gleich, auch wenn du andere Dateien hinzufügst oder löschst.

### Menü-Hintergründe (`guis/`)

Ein Bild, das über das Kisten-Fenster gelegt wird. Items liegen weiterhin darüber und bleiben klickbar.

- Breite **176 px**, Höhe `17 + Reihen × 18 + 97` (6 Reihen = 222 px, 3 Reihen = 168 px).
- Beste Vorlage: `assets/minecraft/textures/gui/container/generic_54.png` aus der Client-Jar (`.minecraft/versions/26.3/26.3.jar`, mit 7-Zip öffnen). Die Slot-Positionen müssen gleich bleiben (18-px-Raster, erster Slot bei x=7, y=17).
- Die Zahl der Reihen wird aus der Bildhöhe geschlossen. Ist das falsch, setzt du `gui.overrides.<name>.rows`.
- Die horizontale Verschiebung berechnet PackForge aus der Bildbreite. Prüfen kannst du die Lage mit `/pack preview <name>`. Liegt das Bild vertikal daneben, passt du `gui.ascent` oder `gui.overrides.<name>.ascent` an.
- Transparente Bereiche zeigen den Vanilla-Hintergrund darunter. Für unsichtbare Füllslots nimmst du ein Item mit einer komplett transparenten Textur statt grauer Glasscheiben.

### Weitere Dateien

- **`raw/`** wird 1:1 ins Pack kopiert, für alles, was PackForge nicht selbst erzeugt: Sounds, Sprachdateien, Vanilla-Texturen überschreiben. Beispiel: `raw/assets/minecraft/textures/block/stone.png`.
- **`pack.png`** direkt in `assets/` ist das Pack-Icon (64x64 oder 128x128).

## Befehle und Rechte

Alle Befehle brauchen das Recht `packforge.admin`. OPs haben es automatisch.

| Befehl | Was er macht |
| --- | --- |
| `/pack reload` | Liest die `config.yml`, baut das Pack neu und schickt es an alle Online-Spieler, falls sich etwas geändert hat. Meldet die Anzahl der Items, Glyphen, Menüs und Warnungen. |
| `/pack list` | Zeigt alle Items, Glyphen (mit Vorschau) und Menüs |
| `/pack give <item>` | Gibt dir ein Papier mit dem Item-Modell zum Testen |
| `/pack preview <gui>` | Öffnet ein leeres Inventar mit dem Menü-Hintergrund, um die Lage zu prüfen |
| `/pack send` | Schickt dir das Pack erneut |
| `/pack send <spieler>` | Schickt es anderen Spielern, Selektoren wie `@a` gehen auch |

Tab-Vervollständigung schlägt bei `give` und `preview` die vorhandenen Namen vor.

## Hosting und Auslieferung

Der Client lädt das Pack über eine URL herunter. Welche URL in `host.public-url` gehört, hängt davon ab, wer spielt:

| Situation | `host.public-url` | Was zu tun ist |
| --- | --- | --- |
| Du testest allein auf deinem PC | `http://localhost:8163/pack.zip` | Nichts, ist der Standard |
| Freunde im selben WLAN | `http://<deine-LAN-IP>:8163/pack.zip` | LAN-IP mit `ipconfig` herausfinden, Port in der Windows-Firewall erlauben |
| Spieler aus dem Internet | `http://<öffentliche-IP-oder-Domain>:8163/pack.zip` | Port 8163 (TCP) im Router weiterleiten, wie den Minecraft-Port |
| Eigener Webspace oder GitHub Release | Download-Link der Zip | `host.enabled: false` setzen und `output/pack.zip` nach jeder Änderung dort hochladen |

Wichtig:

- Die URL muss ein **direkter Download** sein. Vorschauseiten wie bei Google Drive oder Dropbox funktionieren nicht.
- **Mehrere Server auf einem PC** brauchen unterschiedliche `host.port`-Werte, z.B. 8163 und 8164.
- **Caching:** PackForge hängt `?v=<hash>` an die URL und schickt den SHA-1-Hash mit. Der Client lädt nur neu, wenn sich der Inhalt geändert hat. Unveränderte Assets ergeben immer denselben Hash, also keinen erneuten Download.
- **Pflicht-Pack:** Mit `send.required: true` trennt sich der Client selbst, wenn der Spieler ablehnt. Fehlgeschlagene Downloads stehen mit Spielername und Grund in der Konsole.
- Das Pack wird beim Join geschickt, also nach dem Betreten der Welt. Der Spieler sieht kurz den Ladebildschirm des Packs.

## API für eigene Plugins

### Einbinden

1. Im PackForge-Ordner einmal `gradlew publishToMavenLocal` ausführen. Nach Änderungen an PackForge wiederholen.
2. Im anderen Projekt in `build.gradle.kts`:

   ```kotlin
   repositories { mavenLocal() }
   dependencies { compileOnly("me.mangokevin:packforge:1.0-SNAPSHOT") }
   ```
3. In dessen `paper-plugin.yml`, damit PackForge zuerst geladen wird:

   ```yaml
   dependencies:
     server:
       PackForge:
         load: BEFORE
         required: true
   ```

### Methoden

Alles läuft über `PackApi pack = PackForge.api();`.

| Methode | Rückgabe | Wofür |
| --- | --- | --- |
| `glyph("coin")` | `Component` | Glyphe für Texte, weiß und ohne Schatten |
| `guiTitle("shop", title)` | `Component` | Kompletter Inventartitel mit Hintergrund und deinem Text |
| `guiBackground("shop")` | `Component` | Nur der Hintergrund, Text danach sitzt an der normalen Titelposition |
| `guiRows("shop")` | `int` | Reihen, für die das Bild gemalt ist |
| `applyModel(item, "icon/coin")` | `ItemStack` | Setzt das Item-Modell auf ein bestehendes Item |
| `itemModel("icon/coin")` | `NamespacedKey` | Key für `ItemMeta#setItemModel` |
| `shift(-8)` | `Component` | Unsichtbarer Abstand in Pixeln, negativ nach links (bis ±1023) |
| `tags()` | `TagResolver` | MiniMessage-Tags (siehe unten) |
| `hasGlyph` / `hasGui` / `hasItem` | `boolean` | Prüfen, ob ein Name existiert |
| `glyphNames` / `guiNames` / `itemNames` | `Set<String>` | Alle vorhandenen Namen |

Unbekannte Namen werfen keinen Fehler. Es kommt einmal eine Warnung in die Konsole, und statt der Glyphe erscheint ein rotes `?` (bei Menüs kein Hintergrund).

### MiniMessage-Tags

Wenn dein Plugin Texte aus Configs mit MiniMessage liest, gibst du `pack.tags()` als Resolver mit. Dann gehen in jeder Config:

| Tag | Ergebnis |
| --- | --- |
| `<glyph:coin>` | Glyphe `coin` |
| `<gui:shop>` | Menü-Hintergrund `shop` |
| `<shift:-8>` | 8 px nach links |

### Beispiele

```java
PackApi pack = PackForge.api();

// Menü mit eigenem Hintergrund
Inventory inv = Bukkit.createInventory(holder, pack.guiRows("shop") * 9,
        pack.guiTitle("shop", Component.text("Shop", NamedTextColor.DARK_GRAY)));

// Icon-Item
ItemStack coin = pack.applyModel(new ItemStack(Material.PAPER), "icon/coin");

// Text mit Glyphe
player.sendMessage(Component.text().append(pack.glyph("coin")).append(Component.text(" 250 Coins")));

// Config-Text mit Tags, z.B. für Scoreboard oder Tablist
Component line = MiniMessage.miniMessage().deserialize("<glyph:coin> <gold><coins>",
        TagResolver.resolver(pack.tags(), Placeholder.unparsed("coins", "250")));
```

Tipp: Rufe die API bei jedem Aufbau eines Menüs oder Texts neu auf, statt Components dauerhaft zu speichern. Dann wirken sich `/pack reload`-Änderungen sofort aus.

## Fehlerbehebung

| Symptom | Ursache und Lösung |
| --- | --- |
| Datei taucht in `/pack list` nicht auf | Ungültiger Name (Großbuchstaben, Leerzeichen, Umlaute) oder keine echte PNG. Die Warnung steht in der Konsole. |
| Lila-schwarzes Item | Pack nicht geladen, oder der Name in `applyModel` passt nicht. `/pack give` testet das Modell direkt. |
| Glyphe ist ein Kästchen | Pack nicht geladen. Im Client unter Optionen → Ressourcenpakete prüfen, ob das Server-Pack aktiv ist. |
| Rotes `?` statt Glyphe | Name existiert nicht. Warnung in der Konsole, `/pack list` zeigt die gültigen Namen. |
| Menübild zu weit oben oder unten | `gui.ascent` bzw. `gui.overrides.<name>.ascent` anpassen, `/pack reload`, `/pack preview` |
| Menübild links oder rechts verschoben | Bild ist nicht 176 px breit, oder `gui.title-offset` passt nicht |
| Falsche Inventargröße bei `/pack preview` | `gui.overrides.<name>.rows` setzen |
| Download schlägt fehl (Konsole: `FAILED_DOWNLOAD`, `INVALID_URL`) | `host.public-url` aus Sicht des Spielers nicht erreichbar. `localhost` geht nur auf deinem eigenen PC (siehe Hosting). |
| Konsole: Webserver kann nicht starten | Port belegt, oft durch einen zweiten Server. Anderen `host.port` wählen. |
| Client meldet inkompatibles Pack | `pack.min-format`/`max-format` passt nicht zur Client-Version |
| Änderungen kommen nicht an | `/pack reload` vergessen, oder bei externem Hosting die neue Zip nicht hochgeladen |

Wenn nichts davon hilft: `output/pack.zip` öffnen und prüfen, ob die Datei unter `assets/<namespace>/...` liegt. Das Client-Log (`.minecraft/logs/latest.log`) nennt fehlende oder fehlerhafte Pack-Dateien beim Namen.
