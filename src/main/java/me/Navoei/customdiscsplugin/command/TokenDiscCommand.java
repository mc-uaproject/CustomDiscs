package me.Navoei.customdiscsplugin.command;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import me.Navoei.customdiscsplugin.CustomDiscs;
import me.Navoei.customdiscsplugin.language.Lang;
import me.Navoei.customdiscsplugin.utils.TokenUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.JukeboxPlayableComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.codehaus.plexus.util.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Permission("customdiscs.tokendisc")
@Command(name = "tokendisc", aliases = {"td"})
public class TokenDiscCommand {

    private final CustomDiscs plugin;

    public TokenDiscCommand(CustomDiscs plugin) {
        this.plugin = plugin;
    }

    @Execute
    private void onCommandPlayer(@Context Player player, @Arg String url, @Arg String filename, @Arg String songName) {
        if (TokenUtil.checkForToken(player)) {
            player.sendMessage(createEnhancedMessage("🎫", Lang.TOKEN_FOUND.toString(), NamedTextColor.GREEN));
            player.sendMessage(createEnhancedMessage("🎵", Lang.CREATING_DISC.replace("%song_name%", songName), NamedTextColor.YELLOW));

            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    URL fileURL = URI.create(url).toURL();
                    if (filename.contains("../")) {
                        player.sendMessage(createEnhancedMessage("❌", Lang.INVALID_PATH_TRAVERSAL.toString(), NamedTextColor.RED));
                        return;
                    }

                    if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") && !getFileExtension(filename).equals("flac")) {
                        player.sendMessage(createEnhancedMessage("⚠️", Lang.INVALID_FORMAT_DETAILED.toString(), NamedTextColor.RED));
                        return;
                    }

                    player.sendMessage(createEnhancedMessage("💼", Lang.STARTING_DOWNLOAD.replace("%filename%", filename), NamedTextColor.YELLOW));
                    player.sendMessage(createEnhancedMessage("🔗", Lang.URL_INFO.replace("%url%", url), NamedTextColor.GRAY));
                    player.sendMessage(createEnhancedMessage("💾", Lang.FILENAME_INFO.replace("%filename%", filename), NamedTextColor.GRAY));

                    URLConnection connection = fileURL.openConnection();
                    if (connection != null) {
                        long size = connection.getContentLengthLong() / 1048576;
                        if (size > this.plugin.getConfig().getInt("max-download-size", 50)) {
                            player.sendMessage(createEnhancedMessage("🚫", Lang.FILE_TOO_LARGE.replace("%max_download_size%", String.valueOf(this.plugin.getConfig().getInt("max-download-size", 50))), NamedTextColor.RED));
                            return;
                        }
                        if (size > 0) {
                            player.sendMessage(createEnhancedMessage("📊", Lang.FILE_SIZE_DISPLAY.replace("%size%", String.valueOf(size)), NamedTextColor.BLUE));
                        }
                    }

                    Path downloadPath = Path.of(this.plugin.getDataFolder().getPath(), "musicdata", filename);
                    File downloadFile = new File(downloadPath.toUri());
                    FileUtils.copyURLToFile(fileURL, downloadFile);

                    player.sendMessage(createEnhancedMessage("✨", Lang.DOWNLOAD_SUCCESS.toString(), NamedTextColor.GREEN));
                    player.sendMessage(createEnhancedMessage("💾", Lang.FILE_SAVED_AS.replace("%filename%", filename), NamedTextColor.GRAY));
                    player.sendMessage(createEnhancedMessage("🎵", Lang.CREATING_DISC_ITEM.toString(), NamedTextColor.YELLOW));

                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        ItemStack disc = new ItemStack(Material.MUSIC_DISC_CHIRP);
                        ItemMeta meta = disc.getItemMeta();
                        @Nullable List<Component> itemLore = new ArrayList<>();
                        final TextComponent customLoreSong = Component.text().decoration(TextDecoration.ITALIC, false).content(songName).color(NamedTextColor.GRAY).build();
                        itemLore.add(customLoreSong);
                        meta.lore(itemLore);

                        JukeboxPlayableComponent jpc = meta.getJukeboxPlayable();
                        jpc.setShowInTooltip(false);
                        meta.setJukeboxPlayable(jpc);

                        PersistentDataContainer data = meta.getPersistentDataContainer();
                        data.set(new NamespacedKey(this.plugin, "customdisc"), PersistentDataType.STRING, filename);
                        disc.setItemMeta(meta);

                        if (player.getInventory().firstEmpty() == -1) {
                            player.getWorld().dropItem(player.getLocation(), disc);
                            player.sendMessage(createEnhancedMessage("⚠️", Lang.INVENTORY_FULL.toString(), NamedTextColor.YELLOW));
                        } else {
                            player.getInventory().addItem(disc);
                            player.sendMessage(createEnhancedMessage("✅", Lang.DISC_ADDED.toString(), NamedTextColor.GREEN));
                        }

                        TokenUtil.removeToken(player);
                        player.sendMessage(createEnhancedMessage("🎫", Lang.TOKEN_USED.toString(), NamedTextColor.GOLD));
                        player.sendMessage(createEnhancedMessage("💾", Lang.CREATE_FILENAME.replace("%filename%", filename), NamedTextColor.GRAY));
                        player.sendMessage(createEnhancedMessage("🎵", Lang.CREATE_CUSTOM_NAME.replace("%custom_name%", songName), NamedTextColor.GRAY));
                        player.sendMessage(createEnhancedMessage("✨", Lang.DISC_CREATED.toString(), NamedTextColor.GREEN));
                    });

                } catch (IOException e) {
                    player.sendMessage(createEnhancedMessage("🚫", Lang.DOWNLOAD_ACCESS_ERROR.toString(), NamedTextColor.RED));
                    player.sendMessage(createEnhancedMessage("⚠️", Lang.ERROR_DETAILS.replace("%error%", e.getMessage()), NamedTextColor.YELLOW));
                    player.sendMessage(createEnhancedMessage("🔍", Lang.CHECK_URL.toString(), NamedTextColor.GRAY));
                    this.plugin.getLogger().warning("TokenDisc creation failed: " + e.getMessage());
                }
            });
        } else {
            player.sendMessage(createEnhancedMessage("❌", Lang.NO_TOKEN.toString(), NamedTextColor.RED));
            player.sendMessage(createEnhancedMessage("🎫", Lang.TOKEN_REQUIRED.toString(), NamedTextColor.YELLOW));
        }
    }

    private void sendMessage(Player player, String key, String defaultValue) {
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + defaultValue));
    }

    private void sendMessage(Player player, String key, String defaultValue, String placeholder, String replacement) {
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + defaultValue.replace(placeholder, replacement)));
    }

    private String getFileExtension(String s) {
        int index = s.lastIndexOf(".");
        if (index > 0) {
            return s.substring(index + 1);
        } else {
            return "";
        }
    }
    
    private Component createEnhancedMessage(String emoji, String message, NamedTextColor color) {
        Component prefix = LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX.toString());
        Component enhancedMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        
        return Component.text()
            .append(prefix)
            .append(Component.text(emoji + " ", NamedTextColor.GOLD))
            .append(enhancedMessage)
            .build();
    }
}