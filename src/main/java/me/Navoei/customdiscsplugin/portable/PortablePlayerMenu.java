package me.Navoei.customdiscsplugin.portable;

import me.Navoei.customdiscsplugin.command.CustomDiscCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PortablePlayerMenu implements InventoryHolder {

    private final CustomDiscCommand customDiscCommand;
    private final Inventory inventory;
    private final PortablePlayerManager portablePlayerManager;
    private static final int STOP_SLOT = 49;

    public PortablePlayerMenu(CustomDiscCommand customDiscCommand, PortablePlayerManager portablePlayerManager) {
        this.customDiscCommand = customDiscCommand;
        this.portablePlayerManager = portablePlayerManager;
        this.inventory = Bukkit.createInventory(this, 9*6, Component.text("Портативний програвач"));
    }

    public void initializeItems(Player player) {
        List<ItemStack> discs = Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .filter(customDiscCommand::isCustomDisc)
                .toList();

        for (int i = 0; i < discs.size() && i < inventory.getSize(); i++) {
            inventory.setItem(i, discs.get(i));
        }

        ItemStack stopButton = createButton(
                Material.RED_WOOL,
                "§cЗупинити",
                Collections.singletonList("§7Натисніть для зупинки програвання")
        );


        inventory.setItem(STOP_SLOT, stopButton);
    }

    private ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(lore.stream().map(Component::text).collect(Collectors.toList()));
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player) {
        initializeItems(player);
        player.openInventory(inventory);
    }

    public void close(Player player) {
        player.closeInventory();
    }

    public void updateItems(Player player) {
        inventory.clear();
        initializeItems(player);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
