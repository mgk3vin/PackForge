package me.mangokevin.packforge;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Remembers which character each glyph and gui got, so codes stay the same
 * across rebuilds even when files are added or removed.
 */
final class CharTable {

    private final Map<String, Character> chars;
    private char next;

    private CharTable(Map<String, Character> chars) {
        this.chars = chars;
        this.next = (char) chars.values().stream().mapToInt(c -> c + 1).max().orElse(PackBuilder.FIRST_CHAR);
    }

    static CharTable load(Path file) {
        Map<String, Character> chars = new TreeMap<>();
        if (Files.isRegularFile(file)) {
            for (String line : YamlConfiguration.loadConfiguration(file.toFile()).getStringList("chars")) {
                int split = line.lastIndexOf('=');
                if (split > 0) {
                    chars.put(line.substring(0, split), (char) Integer.parseInt(line.substring(split + 1), 16));
                }
            }
        }
        return new CharTable(chars);
    }

    char get(String key) {
        return chars.computeIfAbsent(key, k -> {
            if (next >= PackBuilder.SHIFT_NEG_BASE) {
                throw new IllegalStateException("Out of private use characters");
            }
            return next++;
        });
    }

    void save(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        chars.forEach((key, c) -> lines.add(key + "=" + Integer.toHexString(c).toUpperCase()));
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of("Assigned font characters. Delete an entry to free its character."));
        yaml.set("chars", lines);
        Files.createDirectories(file.getParent());
        yaml.save(file.toFile());
    }
}
