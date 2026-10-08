package me.mangokevin.packforge;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.logging.Logger;

/**
 * What other plugins use: glyphs, menu titles and item models by name.
 *
 * <pre>
 * PackApi pack = PackForge.api();
 * pack.glyph("coin")                         // Component for chat, scoreboard, tablist
 * pack.guiTitle("shop", Component.text("Shop")) // inventory title with background
 * pack.applyModel(item, "icon/coin")         // custom item texture
 * MiniMessage: &lt;glyph:coin&gt;  &lt;gui:shop&gt;  &lt;shift:-8&gt;
 * </pre>
 */
public final class PackApi {

    private final PackForge plugin;
    private final Logger logger;
    private final Set<String> reportedMissing = java.util.concurrent.ConcurrentHashMap.newKeySet();

    PackApi(PackForge plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    private Assets assets() {
        return plugin.assets();
    }

    private String namespace() {
        return plugin.settings().namespace();
    }

    /** A glyph from assets/glyphs, white and without text shadow so its colors stay untouched. */
    public Component glyph(String name) {
        Character c = assets().glyphs().get(name);
        if (c == null) {
            missing("glyph", name);
            return Component.text("?", NamedTextColor.RED);
        }
        return Component.text(c)
                .font(Key.key(namespace(), "glyphs"))
                .color(NamedTextColor.WHITE)
                .shadowColor(ShadowColor.none());
    }

    /** Invisible space that moves the following text by {@code pixels} (negative = left). */
    public Component shift(int pixels) {
        if (pixels == 0) return Component.empty();
        char base = pixels < 0 ? PackBuilder.SHIFT_NEG_BASE : PackBuilder.SHIFT_POS_BASE;
        int amount = Math.min(Math.abs(pixels), (1 << PackBuilder.SHIFT_BITS) - 1);
        StringBuilder text = new StringBuilder();
        for (int bit = 0; bit < PackBuilder.SHIFT_BITS; bit++) {
            if ((amount & (1 << bit)) != 0) text.append((char) (base + bit));
        }
        return Component.text(text.toString()).font(Key.key(namespace(), "shift"));
    }

    /** Menu background from assets/guis, positioned so the title text after it sits where it normally would. */
    public Component guiBackground(String name) {
        Assets.Gui gui = assets().guis().get(name);
        if (gui == null) {
            missing("gui", name);
            return Component.empty();
        }
        int offset = plugin.settings().guiTitleOffset();
        return Component.text()
                .append(shift(-offset))
                .append(Component.text(gui.character())
                        .font(Key.key(namespace(), "guis"))
                        .color(NamedTextColor.WHITE)
                        .shadowColor(ShadowColor.none()))
                .append(shift(offset - gui.advance()))
                .build();
    }

    /** Inventory title: background image plus your title text. */
    public Component guiTitle(String name, Component title) {
        return Component.text().append(guiBackground(name)).append(title).build();
    }

    /** Inventory rows the gui image was drawn for (from its height, or gui.overrides.&lt;name&gt;.rows). */
    public int guiRows(String name) {
        Assets.Gui gui = assets().guis().get(name);
        return gui == null ? 6 : gui.rows();
    }

    /** Key for the item_model component, e.g. custom:icon/coin. */
    public NamespacedKey itemModel(String name) {
        if (!assets().items().contains(name)) missing("item", name);
        return new NamespacedKey(namespace(), name);
    }

    /** Gives the item the custom model. Returns the same stack for chaining. */
    public ItemStack applyModel(ItemStack item, String name) {
        NamespacedKey key = itemModel(name);
        item.editMeta(meta -> meta.setItemModel(key));
        return item;
    }

    public boolean hasGlyph(String name) {
        return assets().glyphs().containsKey(name);
    }

    public boolean hasGui(String name) {
        return assets().guis().containsKey(name);
    }

    public boolean hasItem(String name) {
        return assets().items().contains(name);
    }

    public Set<String> glyphNames() {
        return assets().glyphs().keySet();
    }

    public Set<String> guiNames() {
        return assets().guis().keySet();
    }

    public Set<String> itemNames() {
        return assets().items();
    }

    /** MiniMessage tags: &lt;glyph:name&gt;, &lt;gui:name&gt;, &lt;shift:pixels&gt;. */
    public TagResolver tags() {
        return TagResolver.resolver(
                TagResolver.resolver("glyph", (args, context) ->
                        Tag.selfClosingInserting(glyph(args.popOr("glyph needs a name").value()))),
                TagResolver.resolver("gui", (args, context) ->
                        Tag.selfClosingInserting(guiBackground(args.popOr("gui needs a name").value()))),
                TagResolver.resolver("shift", (args, context) ->
                        Tag.selfClosingInserting(shift(args.popOr("shift needs pixels").asInt().orElse(0))))
        );
    }

    void clearMissing() {
        reportedMissing.clear();
    }

    private void missing(String type, String name) {
        if (reportedMissing.add(type + ":" + name)) {
            logger.warning("Unknown " + type + " '" + name + "'. Check the assets folder and /pack reload.");
        }
    }
}
