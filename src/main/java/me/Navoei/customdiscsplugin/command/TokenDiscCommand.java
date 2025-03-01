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
                        } else {
                            player.getInventory().addItem(disc);
                        }

                        TokenUtil.removeToken(player);
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.CREATE_FILENAME.toString().replace("%filename%", filename)));
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.CREATE_CUSTOM_NAME.toString().replace("%custom_name%", songName)));
                    });

                } catch (IOException e) {
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.DOWNLOAD_ERROR.toString()));
                    e.printStackTrace();
                }
            });
        } else {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.NO_TOKEN.toString()));
        }
    }

    private String getFileExtension(String s) {
        int index = s.lastIndexOf(".");
        if (index > 0) {
            return s.substring(index + 1);
        } else {
            return "";
        }
    }

}
