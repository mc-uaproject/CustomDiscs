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
            commandSender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.PLAYER_NOT_FOUND.toString()));
        } else {
            TokenUtil.grantToken(player);
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.TOKEN_GRANTED.toString()));
            commandSender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.TOKEN_GRANTED_OTHER.toString().replace("%player%", player.getName())));
        }
    }

    @Execute(name = "range")
    private void onCommandRange(@Context Player player, @Arg float range) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isCustomDisc(item)) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.NOT_HOLDING_DISC.toString()));
            return;
        }

        if (range < 1 || range > this.plugin.musicDiscMaxDistance) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.INVALID_RANGE.toString().replace("%range_value%", Float.toString(this.plugin.musicDiscMaxDistance))));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(new NamespacedKey(this.plugin, "range"), PersistentDataType.FLOAT, range);
        player.getInventory().getItemInMainHand().setItemMeta(meta);
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.CREATE_CUSTOM_RANGE.toString().replace("%custom_range%", Float.toString(range))));
    }

    @Execute(name = "download")
    private int onCommandDownload(@Context Player player, @Arg String url, @Arg String filename) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                URL fileURL = new URL(url);
                if (filename.contains("../")) {
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.INVALID_FILENAME.toString()));
                    return;
                }

                if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") && !getFileExtension(filename).equals("flac")) {
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.INVALID_FORMAT.toString()));
                    return;
                }

                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.DOWNLOADING_FILE.toString()));

                URLConnection connection = fileURL.openConnection();
                if (connection != null) {
                    long size = connection.getContentLengthLong() / 1048576;
                    if (size > this.plugin.getConfig().getInt("max-download-size", 50)) {
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.FILE_TOO_LARGE.toString().replace("%max_download_size%", String.valueOf(this.plugin.getConfig().getInt("max-download-size", 50)))));
                        return;
                    }
                }

                Path downloadPath = Path.of(this.plugin.getDataFolder().getPath(), "musicdata", filename);
                File downloadFile = new File(downloadPath.toUri());
                FileUtils.copyURLToFile(fileURL, downloadFile);

                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.SUCCESSFUL_DOWNLOAD.toString().replace("%file_path%", "plugins/CustomDiscs/musicdata/" + filename)));
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.CREATE_DISC.toString().replace("%filename%", filename)));
            } catch (IOException e) {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.DOWNLOAD_ERROR.toString()));
                e.printStackTrace();
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

    @Execute
    private int onCommandPlayer(@Context Player player) {
        FileConfiguration config = this.plugin.getConfig();
        for (String message : config.getStringList("help")) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
        }

        return 1;
    }

}
