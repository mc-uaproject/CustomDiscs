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
            sendMessage(player, "TOKEN_FOUND", "&aТокен знайдено! Обробка вашого запиту...");
            sendMessage(player, "CREATING_DISC", "&7Створення диску з назвою: &f{song_name}", "{song_name}", songName);

            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    URL fileURL = new URL(url);
                    if (filename.contains("../")) {
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.INVALID_FILENAME.toString()));
                        return;
                    }

                    if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") && !getFileExtension(filename).equals("flac")) {
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.INVALID_FORMAT.toString()));
                        sendMessage(player, "SUPPORTED_FORMATS", "&7Підтримувані формати: &fwav, mp3, flac");
                        return;
                    }

                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.DOWNLOADING_FILE.toString()));
                    sendMessage(player, "URL_INFO", "&7URL: &f{url}", "{url}", url);
                    sendMessage(player, "FILENAME_INFO", "&7Ім'я файлу: &f{filename}", "{filename}", filename);

                    URLConnection connection = fileURL.openConnection();
                    if (connection != null) {
                        long size = connection.getContentLengthLong() / 1048576;
                        if (size > this.plugin.getConfig().getInt("max-download-size", 50)) {
                            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.FILE_TOO_LARGE.replace("%max_download_size%", String.valueOf(this.plugin.getConfig().getInt("max-download-size", 50)))));
                            return;
                        }
                        if (size > 0) {
                            sendMessage(player, "FILE_SIZE_INFO", "&7Розмір файлу: &f{size} МБ", "{size}", String.valueOf(size));
                        }
                    }

                    Path downloadPath = Path.of(this.plugin.getDataFolder().getPath(), "musicdata", filename);
                    File downloadFile = new File(downloadPath.toUri());
                    FileUtils.copyURLToFile(fileURL, downloadFile);

                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.SUCCESSFUL_DOWNLOAD.replace("%file_path%", "plugins/CustomDiscs/musicdata/" + filename)));
                    sendMessage(player, "CREATING_DISC_ITEM", "&aСтворення елементу диску...");

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
                            sendMessage(player, "INVENTORY_FULL", "&cІнвентар заповнений! &aДиск викинуто біля вас.");
                        } else {
                            player.getInventory().addItem(disc);
                            sendMessage(player, "DISC_ADDED", "&aДиск додано до вашого інвентарю!");
                        }

                        TokenUtil.removeToken(player);
                        sendMessage(player, "TOKEN_USED", "&aТокен використано.");
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.CREATE_FILENAME.replace("%filename%", filename)));
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.CREATE_CUSTOM_NAME.replace("%custom_name%", songName)));
                        sendMessage(player, "DISC_CREATED", "&aКастомний диск успішно створено!");
                    });

                } catch (IOException e) {
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.DOWNLOAD_ERROR.toString()));
                    sendMessage(player, "ERROR_DETAILS", "&cДеталі помилки: &f{error}", "{error}", e.getMessage());
                    sendMessage(player, "CHECK_URL", "&cПеревірте, будь ласка, що URL-адреса правильна і файл доступний.");
                    e.printStackTrace();
                }
            });
        } else {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.NO_TOKEN.toString()));
            sendMessage(player, "TOKEN_REQUIRED", "&cВам потрібен токен, щоб створити кастомний диск. Запитайте адміністратора.");
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
}