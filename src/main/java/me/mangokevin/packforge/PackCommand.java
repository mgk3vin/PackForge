package me.mangokevin.packforge;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Supplier;

final class PackCommand {

    static final String PERMISSION = "packforge.admin";

    private PackCommand() {
    }

    static LiteralCommandNode<CommandSourceStack> create(PackForge plugin) {
        PackApi api = PackForge.api();
        return Commands.literal("pack")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.literal("reload").executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    try {
                        PackBuilder.Result result = plugin.reload();
                        sender.sendMessage(Component.text("Pack rebuilt: " + result.assets().items().size() + " items, "
                                + result.assets().glyphs().size() + " glyphs, " + result.assets().guis().size() + " guis"
                                + (result.warnings() > 0 ? ", " + result.warnings() + " warnings (see console)" : ""),
                                result.warnings() > 0 ? NamedTextColor.YELLOW : NamedTextColor.GREEN));
                    } catch (IOException e) {
                        sender.sendMessage(Component.text("Build failed: " + e.getMessage(), NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("send")
                        .executes(context -> {
                            if (context.getSource().getSender() instanceof Player player) plugin.send(player);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("targets", ArgumentTypes.players()).executes(context -> {
                            Collection<Player> targets = context.getArgument("targets", PlayerSelectorArgumentResolver.class)
                                    .resolve(context.getSource());
                            targets.forEach(plugin::send);
                            context.getSource().getSender().sendMessage(Component.text(
                                    "Sent pack to " + targets.size() + " player(s).", NamedTextColor.GREEN));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("list").executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    sender.sendMessage(Component.text("Items: ", NamedTextColor.GOLD)
                            .append(Component.text(String.join(", ", api.itemNames()), NamedTextColor.GRAY)));
                    Component glyphs = Component.text("Glyphs: ", NamedTextColor.GOLD);
                    for (String name : api.glyphNames()) {
                        glyphs = glyphs.append(api.glyph(name)).append(Component.text(" " + name + "  ", NamedTextColor.GRAY));
                    }
                    sender.sendMessage(glyphs);
                    sender.sendMessage(Component.text("Guis: ", NamedTextColor.GOLD)
                            .append(Component.text(String.join(", ", api.guiNames()), NamedTextColor.GRAY)));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("give")
                        .then(Commands.argument("item", StringArgumentType.greedyString())
                                .suggests(suggest(api::itemNames))
                                .executes(context -> {
                                    Player player = player(context);
                                    if (player == null) return 0;
                                    String name = StringArgumentType.getString(context, "item");
                                    ItemStack item = api.applyModel(new ItemStack(Material.PAPER), name);
                                    item.editMeta(meta -> meta.customName(Component.text(name)));
                                    player.getInventory().addItem(item);
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("preview")
                        .then(Commands.argument("gui", StringArgumentType.greedyString())
                                .suggests(suggest(api::guiNames))
                                .executes(context -> {
                                    Player player = player(context);
                                    if (player == null) return 0;
                                    String name = StringArgumentType.getString(context, "gui");
                                    player.openInventory(Bukkit.createInventory(null, api.guiRows(name) * 9,
                                            api.guiTitle(name, Component.text(name, NamedTextColor.DARK_GRAY))));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }

    private static Player player(CommandContext<CommandSourceStack> context) {
        return context.getSource().getSender() instanceof Player player ? player : null;
    }

    private static SuggestionProvider<CommandSourceStack> suggest(Supplier<Collection<String>> names) {
        return (context, builder) -> {
            String input = builder.getRemaining();
            names.get().stream().filter(name -> name.startsWith(input)).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
