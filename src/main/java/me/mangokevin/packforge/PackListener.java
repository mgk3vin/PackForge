package me.mangokevin.packforge;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

final class PackListener implements Listener {

    private final PackForge plugin;

    PackListener(PackForge plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.settings().sendOnJoin()) {
            plugin.send(event.getPlayer());
        }
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        switch (event.getStatus()) {
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> plugin.getLogger().warning(
                    event.getPlayer().getName() + " could not load the pack (" + event.getStatus()
                            + "). Check host.public-url in config.yml.");
            default -> {
            }
        }
    }
}
