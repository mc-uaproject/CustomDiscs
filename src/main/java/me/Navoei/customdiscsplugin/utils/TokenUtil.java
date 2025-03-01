package me.Navoei.customdiscsplugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class TokenUtil {

    public static boolean checkForToken(Player player) {
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null) {
                if (itemStack.hasItemMeta()) {
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
                    if (persistentDataContainer.has(new NamespacedKey("customdiscs", "token"), PersistentDataType.STRING)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void grantToken(Player player) {
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

    public static void removeToken(Player player) {
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null) {
                if (itemStack.hasItemMeta()) {
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
                    if (persistentDataContainer.has(new NamespacedKey("customdiscs", "token"), PersistentDataType.STRING)) {
                        if (itemStack.getAmount() > 1) {
                            itemStack.setAmount(itemStack.getAmount() - 1);
                        } else {
                            player.getInventory().remove(itemStack);
                        }
                        return;
                    }
                }
            }
        }
    }
}
