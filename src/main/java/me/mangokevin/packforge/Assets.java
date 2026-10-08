package me.mangokevin.packforge;

import java.util.Map;

/**
 * Everything the last build produced, looked up by name.
 *
 * @param glyphs    glyph name -> character in the glyph font
 * @param guis      gui name -> background info
 * @param items     item model names (without namespace)
 */
public record Assets(Map<String, Character> glyphs, Map<String, Gui> guis, java.util.Set<String> items) {

    /**
     * A menu background.
     *
     * @param character character in the gui font
     * @param advance   how far the cursor moves after drawing it (rendered width + 1)
     * @param rows      inventory rows the image was drawn for
     */
    public record Gui(char character, int advance, int rows) {}

    public static Assets empty() {
        return new Assets(Map.of(), Map.of(), java.util.Set.of());
    }
}
