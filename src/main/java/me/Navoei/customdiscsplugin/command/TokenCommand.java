package me.Navoei.customdiscsplugin.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TokenCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        grantToken((Player) commandSender);
        return false;
    }

    public static void grantToken(Player player) {
        if (!player.isOp()) return;
        ItemStack token = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta itemMeta = token.getItemMeta();
        itemMeta.displayName(Component.text("Токен косметики").color(NamedTextColor.GOLD));
        List<Component> lore = List.of(
                Component.text("Використовується, щоб створити платівку із вашим треком").color(NamedTextColor.GRAY),
                Component.text("Приклад: ").color(NamedTextColor.GRAY).append(Component.text("/td https://direct-link-here.mp3 songName.mp3 MyCoolSong!").color(NamedTextColor.GOLD))
        );
        itemMeta.lore(lore);
        itemMeta.getPersistentDataContainer().set(new NamespacedKey("customdiscs", "token"), PersistentDataType.STRING, "token");
        token.setItemMeta(itemMeta);
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), token);
        } else {
            player.getInventory().addItem(token);
        }
    }

}
