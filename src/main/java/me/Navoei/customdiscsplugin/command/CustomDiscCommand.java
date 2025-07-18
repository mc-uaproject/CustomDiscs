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
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Permission("customdiscs.admin")
@Command(name = "customdisc", aliases = {"cd"})
public class CustomDiscCommand {

    private final CustomDiscs plugin;

    public CustomDiscCommand(CustomDiscs plugin) {
        this.plugin = plugin;
    }

    @Execute(name = "token")
    private void onTokenCommand(@Context CommandSender commandSender, @Arg Player player) {
        if (player == null) {
            commandSender.sendMessage(createEnhancedMessage("❌", Lang.PLAYER_NOT_FOUND.toString(), NamedTextColor.RED));
        } else {
            TokenUtil.grantToken(player);
            
            // Enhanced token granted message with visual effects
            player.sendMessage(createEnhancedMessage("🎫", Lang.TOKEN_GRANTED_SUCCESS.toString(), NamedTextColor.GREEN));
            commandSender.sendMessage(createEnhancedMessage("✅", Lang.TOKEN_GRANTED_TO_PLAYER.replace("%player%", player.getName()), NamedTextColor.GREEN));
        }
    }

    @Execute(name = "range")
    private void onCommandRange(@Context Player player, @Arg float range) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isCustomDisc(item)) {
            player.sendMessage(createEnhancedMessage("❌", Lang.MUST_HOLD_CUSTOM_DISC.toString(), NamedTextColor.RED));
            return;
        }

        if (range < 1 || range > this.plugin.musicDiscMaxDistance) {
            player.sendMessage(createEnhancedMessage("⚠️", Lang.INVALID_RANGE_DETAILED.replace("%max_range%", Float.toString(this.plugin.musicDiscMaxDistance)), NamedTextColor.RED));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(new NamespacedKey(this.plugin, "range"), PersistentDataType.FLOAT, range);
        player.getInventory().getItemInMainHand().setItemMeta(meta);
        
        player.sendMessage(createEnhancedMessage("🎵", Lang.RANGE_SET_SUCCESS.replace("%range%", Float.toString(range)), NamedTextColor.GREEN));
    }

    @Execute(name = "download")
    @Permission("customdiscs.download")
    private int onCommandDownload(@Context Player player, @Arg String url, @Arg String filename) {
        // Show initial validation message
        player.sendMessage(createEnhancedMessage("🔍", Lang.VALIDATING_REQUEST.toString(), NamedTextColor.BLUE));
        
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                try {
                    URI uri = new URI(url);
                    URL fileURL = uri.toURL();
                    
                    if (filename.contains("../")) {
                        player.sendMessage(createEnhancedMessage("❌", Lang.INVALID_PATH_TRAVERSAL.toString(), NamedTextColor.RED));
                        return;
                    }

                    String extension = getFileExtension(filename);
                    if (!extension.equals("wav") && !extension.equals("mp3") && !extension.equals("flac")) {
                        player.sendMessage(createEnhancedMessage("⚠️", Lang.INVALID_FORMAT_DETAILED.toString(), NamedTextColor.RED));
                        return;
                    }

                    // Show download starting message with progress indicators
                    player.sendMessage(createEnhancedMessage("💼", Lang.STARTING_DOWNLOAD.replace("%filename%", filename), NamedTextColor.YELLOW));

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
                    File downloadFile = downloadPath.toFile();
                    FileUtils.copyURLToFile(fileURL, downloadFile);

                    // Success messages
                    player.sendMessage(createEnhancedMessage("✨", Lang.DOWNLOAD_SUCCESS.toString(), NamedTextColor.GREEN));
                    player.sendMessage(createEnhancedMessage("💾", Lang.FILE_SAVED_AS.replace("%filename%", filename), NamedTextColor.GRAY));
                    player.sendMessage(createEnhancedMessage("🎵", Lang.CREATE_DISC_INSTRUCTION.replace("%filename%", filename), NamedTextColor.YELLOW));
                        
                } catch (URISyntaxException | MalformedURLException e) {
                    player.sendMessage(createEnhancedMessage("❌", Lang.INVALID_URL_FORMAT.toString(), NamedTextColor.RED));
                    this.plugin.getLogger().warning("Download failed due to invalid URL: " + url + " - " + e.getMessage());
                }
            } catch (IOException e) {
                player.sendMessage(createEnhancedMessage("🚫", Lang.DOWNLOAD_ACCESS_ERROR.toString(), NamedTextColor.RED));
                this.plugin.getLogger().warning("Download failed for file: " + filename + " - " + e.getMessage());
            }
        });

        return 1;
    }

    @Execute(name = "create")
    private int onCommandCreate(@Context Player player, @Arg String filename, @Arg String songName) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isMusicDisc(item)) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.NOT_HOLDING_DISC.toString()));
            return 1;
        }


        if (filename.contains("../")) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.INVALID_FILENAME.toString()));
            return 1;
        }

        File getDirectory = new File(this.plugin.getDataFolder(), "musicdata");
        File songFile = new File(getDirectory.getPath(), filename);
        if (songFile.exists()) {
            if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") && !getFileExtension(filename).equals("flac")) {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.INVALID_FORMAT.toString()));
                return 1;
            }
        } else {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.FILE_NOT_FOUND.toString()));
            return 1;
        }

        ItemStack disc = new ItemStack(player.getInventory().getItemInMainHand());
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
        player.getInventory().getItemInMainHand().setItemMeta(meta);
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.CREATE_FILENAME.toString().replace("%filename%", filename)));
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.CREATE_CUSTOM_NAME.toString().replace("%custom_name%", songName)));
        return 1;
    }

    private String getFileExtension(String s) {
        int index = s.lastIndexOf(".");
        if (index > 0) {
            return s.substring(index + 1);
        } else {
            return "";
        }
    }

    public boolean isCustomDisc(ItemStack item) {
        if (item == null) return false;
        return item.getType().toString().contains("MUSIC_DISC") && item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "customdisc"));
    }

    public static boolean isMusicDisc(ItemStack item) {
        return item.getType().toString().contains("MUSIC_DISC");
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

    @Execute
    private int onCommandPlayer(@Context Player player) {
        FileConfiguration config = this.plugin.getConfig();
        for (String message : config.getStringList("help")) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
        }

        return 1;
    }

}
