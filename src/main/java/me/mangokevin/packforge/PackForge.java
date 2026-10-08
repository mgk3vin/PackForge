package me.mangokevin.packforge;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class PackForge extends JavaPlugin {

    private static PackApi api;

    private PackSettings settings;
    private PackHost host;
    private volatile Assets assets = Assets.empty();
    private volatile byte[] zip;
    private volatile String sha1;

    /** Entry point for other plugins. Available after PackForge is enabled. */
    public static PackApi api() {
        if (api == null) throw new IllegalStateException("PackForge is not enabled");
        return api;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = PackSettings.from(getConfig());
        createAssetFolders();
        api = new PackApi(this);
        try {
            rebuild();
        } catch (IOException e) {
            getLogger().severe("Could not build the resource pack: " + e.getMessage());
        }
        startHost();
        Bukkit.getPluginManager().registerEvents(new PackListener(this), this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(PackCommand.create(this), "Build and send the resource pack"));
    }

    @Override
    public void onDisable() {
        stopHost();
        api = null;
    }

    /** Reloads config.yml, rebuilds the pack and sends it again if it changed. Returns the build result. */
    public PackBuilder.Result reload() throws IOException {
        reloadConfig();
        PackSettings old = settings;
        settings = PackSettings.from(getConfig());
        if (old.hostEnabled() != settings.hostEnabled() || old.hostPort() != settings.hostPort()
                || !old.hostBind().equals(settings.hostBind())) {
            stopHost();
            startHost();
        }
        String oldSha1 = sha1;
        PackBuilder.Result result = rebuild();
        if (!result.sha1().equals(oldSha1)) {
            Bukkit.getOnlinePlayers().forEach(this::send);
        }
        return result;
    }

    private PackBuilder.Result rebuild() throws IOException {
        PackBuilder.Result result = new PackBuilder(assetDir(), getDataPath().resolve("chars.yml"), settings, getLogger()).build();
        Path output = getDataPath().resolve("output").resolve("pack.zip");
        Files.createDirectories(output.getParent());
        Files.write(output, result.zip());
        zip = result.zip();
        sha1 = result.sha1();
        assets = result.assets();
        api.clearMissing();
        getLogger().info("Built pack: " + result.assets().items().size() + " items, " + result.assets().glyphs().size()
                + " glyphs, " + result.assets().guis().size() + " guis, " + result.zip().length / 1024 + " KB, sha1 " + sha1);
        return result;
    }

    /** Sends the current pack to a player. */
    public void send(Player player) {
        if (sha1 == null) return;
        String url = settings.publicUrl();
        // The version parameter makes clients and caches fetch the new file after a rebuild
        url += (url.contains("?") ? "&" : "?") + "v=" + sha1.substring(0, 10);
        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(packId(), URI.create(url), sha1);
        ResourcePackRequest.Builder request = ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .required(settings.required())
                .replace(true);
        if (!settings.prompt().isBlank()) {
            request.prompt(MiniMessage.miniMessage().deserialize(settings.prompt()));
        }
        player.sendResourcePacks(request);
    }

    private UUID packId() {
        return UUID.nameUUIDFromBytes(("PackForge:" + settings.namespace()).getBytes(StandardCharsets.UTF_8));
    }

    private void startHost() {
        if (!settings.hostEnabled()) return;
        try {
            host = new PackHost(settings.hostBind(), settings.hostPort(), () -> zip);
            getLogger().info("Hosting pack on port " + settings.hostPort() + ", players download from " + settings.publicUrl());
        } catch (IOException e) {
            getLogger().severe("Could not start the pack web server on port " + settings.hostPort() + ": " + e.getMessage());
        }
    }

    private void stopHost() {
        if (host != null) {
            host.stop();
            host = null;
        }
    }

    private void createAssetFolders() {
        try {
            for (String folder : new String[]{"items", "glyphs", "guis", "raw"}) {
                Files.createDirectories(assetDir().resolve(folder));
            }
        } catch (IOException e) {
            getLogger().warning("Could not create asset folders: " + e.getMessage());
        }
    }

    Path assetDir() {
        return getDataPath().resolve("assets");
    }

    PackSettings settings() {
        return settings;
    }

    Assets assets() {
        return assets;
    }

    String sha1() {
        return sha1;
    }
}
