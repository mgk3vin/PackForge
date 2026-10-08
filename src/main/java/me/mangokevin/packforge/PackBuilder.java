package me.mangokevin.packforge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Turns the plain asset folders into a resource pack zip.
 *
 * <pre>
 * assets/items/   *.png -> item models (&lt;ns&gt;:&lt;path&gt;), *.json -> custom models (e.g. Blockbench)
 * assets/glyphs/  *.png -> font glyphs for chat, scoreboard, tablist
 * assets/guis/    *.png -> menu backgrounds drawn behind the inventory title
 * assets/raw/     copied into the pack as-is
 * assets/pack.png pack icon
 * </pre>
 */
public final class PackBuilder {

    /** Glyph and gui characters are taken from the Unicode private use area. */
    static final char FIRST_CHAR = '';
    /** Space characters: NEG_BASE + i moves the cursor by -2^i, POS_BASE + i by +2^i. */
    static final char SHIFT_NEG_BASE = '';
    static final char SHIFT_POS_BASE = '';
    static final int SHIFT_BITS = 10;

    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_.\\-/]+");
    private static final LocalDateTime ZIP_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path assetDir;
    private final Path charFile;
    private final PackSettings settings;
    private final Logger logger;

    public PackBuilder(Path assetDir, Path charFile, PackSettings settings, Logger logger) {
        this.assetDir = assetDir;
        this.charFile = charFile;
        this.settings = settings;
        this.logger = logger;
    }

    public record Result(byte[] zip, String sha1, Assets assets, int warnings) {}

    public Result build() throws IOException {
        String ns = settings.namespace();
        Map<String, byte[]> files = new TreeMap<>();
        CharTable chars = CharTable.load(charFile);
        int[] warnings = {0};

        JsonObject pack = new JsonObject();
        pack.addProperty("description", settings.description());
        pack.addProperty("min_format", settings.minFormat());
        pack.addProperty("max_format", settings.maxFormat());
        JsonObject mcmeta = new JsonObject();
        mcmeta.add("pack", pack);
        files.put("pack.mcmeta", json(mcmeta));

        Path icon = assetDir.resolve("pack.png");
        if (Files.isRegularFile(icon)) {
            files.put("pack.png", Files.readAllBytes(icon));
        }

        // raw: copied verbatim
        for (Path file : list(assetDir.resolve("raw"))) {
            files.put(relative(assetDir.resolve("raw"), file), Files.readAllBytes(file));
        }

        // items
        TreeSet<String> items = new TreeSet<>();
        Path itemDir = assetDir.resolve("items");
        List<Path> itemFiles = list(itemDir);
        for (Path file : itemFiles) {
            String rel = relative(itemDir, file);
            if (rel.endsWith(".png.mcmeta")) {
                files.put("assets/" + ns + "/textures/item/" + rel, Files.readAllBytes(file));
                continue;
            }
            String name = stripExtension(rel);
            if (!valid(name, file, warnings)) continue;
            if (rel.endsWith(".png")) {
                files.put("assets/" + ns + "/textures/item/" + rel, Files.readAllBytes(file));
                // Textures only used by custom models start with "_" and get no item of their own
                boolean textureOnly = fileName(name).startsWith("_");
                boolean hasCustomModel = Files.isRegularFile(file.resolveSibling(fileName(name) + ".json"));
                if (!textureOnly && !hasCustomModel) {
                    JsonObject model = new JsonObject();
                    model.addProperty("parent", isHandheld(name) ? "minecraft:item/handheld" : "minecraft:item/generated");
                    JsonObject textures = new JsonObject();
                    textures.addProperty("layer0", ns + ":item/" + name);
                    model.add("textures", textures);
                    files.put("assets/" + ns + "/models/item/" + name + ".json", json(model));
                    files.put("assets/" + ns + "/items/" + name + ".json", itemDefinition(ns, name));
                    items.add(name);
                }
            } else if (rel.endsWith(".json")) {
                byte[] model = Files.readAllBytes(file);
                try {
                    JsonParser.parseString(new String(model, StandardCharsets.UTF_8));
                } catch (RuntimeException e) {
                    warn(warnings, "Skipping invalid JSON model items/" + rel + ": " + e.getMessage());
                    continue;
                }
                files.put("assets/" + ns + "/models/item/" + rel, model);
                files.put("assets/" + ns + "/items/" + rel, itemDefinition(ns, name));
                items.add(name);
            }
        }

        // glyphs
        Map<String, Character> glyphs = new LinkedHashMap<>();
        JsonArray glyphProviders = new JsonArray();
        Path glyphDir = assetDir.resolve("glyphs");
        for (Path file : pngs(glyphDir)) {
            String name = stripExtension(relative(glyphDir, file));
            if (!valid(name, file, warnings) || image(file, warnings) == null) continue;
            char c = chars.get("glyph:" + name);
            int[] size = settings.glyphSize(name);
            files.put("assets/" + ns + "/textures/font/glyphs/" + name + ".png", Files.readAllBytes(file));
            glyphProviders.add(bitmap(ns + ":font/glyphs/" + name + ".png", size[0], size[1], c));
            glyphs.put(name, c);
        }

        // guis
        Map<String, Assets.Gui> guis = new LinkedHashMap<>();
        JsonArray guiProviders = new JsonArray();
        Path guiDir = assetDir.resolve("guis");
        for (Path file : pngs(guiDir)) {
            String name = stripExtension(relative(guiDir, file));
            BufferedImage image;
            if (!valid(name, file, warnings) || (image = image(file, warnings)) == null) continue;
            char c = chars.get("gui:" + name);
            int[] size = settings.guiSize(name, image.getHeight());
            double scale = size[0] / (double) image.getHeight();
            // Same formula the client uses for bitmap glyph widths
            int advance = (int) (0.5 + contentWidth(image) * scale) + 1;
            int rows = Math.clamp(Math.round((image.getHeight() - 114) / 18.0), 1, 6);
            if (settings.guiOverrides() != null) {
                rows = settings.guiOverrides().getInt(name + ".rows", rows);
            }
            files.put("assets/" + ns + "/textures/font/guis/" + name + ".png", Files.readAllBytes(file));
            guiProviders.add(bitmap(ns + ":font/guis/" + name + ".png", size[0], size[1], c));
            guis.put(name, new Assets.Gui(c, advance, rows));
        }

        files.put("assets/" + ns + "/font/glyphs.json", font(glyphProviders));
        files.put("assets/" + ns + "/font/guis.json", font(guiProviders));
        files.put("assets/" + ns + "/font/shift.json", font(shiftProviders()));

        chars.save(charFile);
        byte[] zip = zip(files);
        return new Result(zip, sha1(zip), new Assets(Collections.unmodifiableMap(new TreeMap<>(glyphs)),
                Collections.unmodifiableMap(new TreeMap<>(guis)), Collections.unmodifiableSortedSet(items)), warnings[0]);
    }

    private boolean isHandheld(String name) {
        return settings.handheldFolders().stream().anyMatch(folder -> name.startsWith(folder + "/"));
    }

    private boolean valid(String name, Path file, int[] warnings) {
        if (VALID_NAME.matcher(name).matches()) return true;
        warn(warnings, "Skipping " + assetDir.relativize(file) + ": names may only use a-z 0-9 _ - . /");
        return false;
    }

    private BufferedImage image(Path file, int[] warnings) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(Files.readAllBytes(file)));
        if (image == null) warn(warnings, "Skipping " + assetDir.relativize(file) + ": not a readable PNG");
        return image;
    }

    private void warn(int[] warnings, String message) {
        warnings[0]++;
        logger.warning(message);
    }

    /** Width up to the rightmost column that has a visible pixel. */
    private static int contentWidth(BufferedImage image) {
        for (int x = image.getWidth() - 1; x >= 0; x--) {
            for (int y = 0; y < image.getHeight(); y++) {
                if ((image.getRGB(x, y) >>> 24) != 0) return x + 1;
            }
        }
        return 0;
    }

    private static byte[] itemDefinition(String ns, String name) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", ns + ":item/" + name);
        JsonObject root = new JsonObject();
        root.add("model", model);
        return json(root);
    }

    private static JsonObject bitmap(String file, int height, int ascent, char c) {
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "bitmap");
        provider.addProperty("file", file);
        provider.addProperty("height", height);
        provider.addProperty("ascent", ascent);
        JsonArray chars = new JsonArray();
        chars.add(String.valueOf(c));
        provider.add("chars", chars);
        return provider;
    }

    private static JsonArray shiftProviders() {
        JsonObject advances = new JsonObject();
        for (int i = 0; i < SHIFT_BITS; i++) {
            advances.addProperty(String.valueOf((char) (SHIFT_NEG_BASE + i)), -(1 << i));
            advances.addProperty(String.valueOf((char) (SHIFT_POS_BASE + i)), 1 << i);
        }
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "space");
        provider.add("advances", advances);
        JsonArray providers = new JsonArray();
        providers.add(provider);
        return providers;
    }

    private static byte[] font(JsonArray providers) {
        JsonObject font = new JsonObject();
        font.add("providers", providers);
        return json(font);
    }

    private static byte[] json(JsonObject object) {
        return GSON.toJson(object).getBytes(StandardCharsets.UTF_8);
    }

    /** Fixed timestamps and sorted entries: the same assets always give the same hash. */
    private static byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setTimeLocal(ZIP_TIME);
                zip.putNextEntry(entry);
                zip.write(file.getValue());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Path> list(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile).sorted().toList();
        }
    }

    private static List<Path> pngs(Path dir) throws IOException {
        return list(dir).stream().filter(file -> file.toString().endsWith(".png")).toList();
    }

    private static String relative(Path base, Path file) {
        return base.relativize(file).toString().replace('\\', '/');
    }

    private static String stripExtension(String path) {
        int dot = path.indexOf('.', path.lastIndexOf('/') + 1);
        return dot < 0 ? path : path.substring(0, dot);
    }

    private static String fileName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
