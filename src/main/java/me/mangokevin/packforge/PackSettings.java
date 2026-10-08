package me.mangokevin.packforge;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/** Typed view of config.yml. */
public record PackSettings(
        String namespace,
        String description,
        int minFormat,
        int maxFormat,
        boolean hostEnabled,
        String hostBind,
        int hostPort,
        String publicUrl,
        boolean sendOnJoin,
        boolean required,
        String prompt,
        List<String> handheldFolders,
        int glyphHeight,
        int glyphAscent,
        ConfigurationSection glyphOverrides,
        int guiAscent,
        int guiTitleOffset,
        ConfigurationSection guiOverrides
) {

    public static PackSettings from(FileConfiguration config) {
        return new PackSettings(
                config.getString("namespace", "custom"),
                config.getString("pack.description", "Resources"),
                config.getInt("pack.min-format", 97),
                config.getInt("pack.max-format", 97),
                config.getBoolean("host.enabled", true),
                config.getString("host.bind", "0.0.0.0"),
                config.getInt("host.port", 8163),
                config.getString("host.public-url", "http://localhost:8163/pack.zip"),
                config.getBoolean("send.on-join", true),
                config.getBoolean("send.required", true),
                config.getString("send.prompt", ""),
                config.getStringList("items.handheld-folders"),
                config.getInt("glyphs.height", 8),
                config.getInt("glyphs.ascent", 7),
                config.getConfigurationSection("glyphs.overrides"),
                config.getInt("gui.ascent", 13),
                config.getInt("gui.title-offset", 8),
                config.getConfigurationSection("gui.overrides")
        );
    }

    /** Height/ascent of a glyph, falling back to the defaults. */
    public int[] glyphSize(String name) {
        return size(glyphOverrides, name, glyphHeight, glyphAscent);
    }

    /** Height/ascent of a gui background; height defaults to the image height. */
    public int[] guiSize(String name, int imageHeight) {
        return size(guiOverrides, name, imageHeight, guiAscent);
    }

    private static int[] size(ConfigurationSection overrides, String name, int height, int ascent) {
        if (overrides != null && overrides.isConfigurationSection(name)) {
            ConfigurationSection section = overrides.getConfigurationSection(name);
            height = section.getInt("height", height);
            ascent = section.getInt("ascent", ascent);
        }
        return new int[]{height, Math.min(ascent, height)};
    }
}
