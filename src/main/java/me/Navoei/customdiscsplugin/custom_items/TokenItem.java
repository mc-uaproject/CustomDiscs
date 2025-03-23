package me.Navoei.customdiscsplugin.custom_items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class TokenItem {
    public static ItemStack getToken() {
        ItemStack token = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta itemMeta = token.getItemMeta();
        itemMeta.displayName(Component.text("Токен косметики").color(NamedTextColor.GOLD));
        List<Component> lore = List.of(
                Component.text("Використовується, щоб створити платівку із вашим треком").color(NamedTextColor.GRAY),
                Component.text("Приклад: ").color(NamedTextColor.GRAY)
                        .append(Component.text("/td https://direct-link-here.mp3 songName.mp3 MyCoolSong!").color(NamedTextColor.GOLD))
        );
        itemMeta.lore(lore);

        itemMeta.getPersistentDataContainer().set(new NamespacedKey("customdiscs", "token"), PersistentDataType.STRING, "token");

        token.setItemMeta(itemMeta);
        return token;
    }
}