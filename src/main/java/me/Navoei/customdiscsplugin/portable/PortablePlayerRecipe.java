package me.Navoei.customdiscsplugin.portable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class PortablePlayerRecipe {

    private final JavaPlugin pluginPortablePlayerRecipe;

    public PortablePlayerRecipe(JavaPlugin pluginPortablePlayerRecipe) {
        this.pluginPortablePlayerRecipe = pluginPortablePlayerRecipe;
    }

    public void registerRecipes() {
        ItemStack portablePlayer = new ItemStack(Material.SUGAR);

        ItemMeta portablePlayerMeta = portablePlayer.getItemMeta();
        portablePlayerMeta.getPersistentDataContainer().set(
                new NamespacedKey(pluginPortablePlayerRecipe, "portable_player"),
                PersistentDataType.STRING,
                "true"
        );

        if (portablePlayerMeta != null) {
            portablePlayerMeta.displayName(Component.text("Портативний Програвач").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GREEN));
            portablePlayerMeta.setItemModel(new NamespacedKey(pluginPortablePlayerRecipe, "portable_player"));
            portablePlayer.setItemMeta(portablePlayerMeta);
        }

        NamespacedKey portablePlayerKey = new NamespacedKey(pluginPortablePlayerRecipe, "portable_player");

        ShapedRecipe voidTotemRecipe = new ShapedRecipe(portablePlayerKey, portablePlayer);
        voidTotemRecipe.shape(
                "III",
                "IDI",
                "PPP");

        RecipeChoice planksChoice = new RecipeChoice.MaterialChoice(
                Material.OAK_PLANKS,
                Material.SPRUCE_PLANKS,
                Material.BIRCH_PLANKS,
                Material.JUNGLE_PLANKS,
                Material.ACACIA_PLANKS,
                Material.DARK_OAK_PLANKS
        );

        voidTotemRecipe.setIngredient('D', Material.DIAMOND);
        voidTotemRecipe.setIngredient('P', planksChoice);
        voidTotemRecipe.setIngredient('I', Material.IRON_INGOT);

        Bukkit.addRecipe(voidTotemRecipe);
    }
}