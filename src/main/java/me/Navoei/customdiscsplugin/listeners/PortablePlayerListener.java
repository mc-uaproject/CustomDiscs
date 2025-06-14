package me.Navoei.customdiscsplugin.listeners;

import me.Navoei.customdiscsplugin.CustomDiscs;
import me.Navoei.customdiscsplugin.VoicePlugin;
import me.Navoei.customdiscsplugin.command.CustomDiscCommand;
import me.Navoei.customdiscsplugin.language.Lang;
import me.Navoei.customdiscsplugin.portable.PortablePlayerManager;
import me.Navoei.customdiscsplugin.portable.PortablePlayerMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.file.Path;
import java.util.Objects;

public class PortablePlayerListener implements Listener {

    private final CustomDiscs plugin;
    private final CustomDiscCommand customDiscCommand;
    private final PortablePlayerMenu portablePlayerMenu;
    private final PortablePlayerManager portablePlayerManager;
    private CustomDiscs customDiscs = CustomDiscs.getInstance();

    public PortablePlayerListener(CustomDiscs plugin, CustomDiscCommand customDiscCommand, PortablePlayerMenu portablePlayerMenu, PortablePlayerManager portablePlayerManager, CustomDiscs customDiscs) {
        this.plugin = plugin;
        this.customDiscCommand = customDiscCommand;
        this.portablePlayerMenu = portablePlayerMenu;
        this.portablePlayerManager = portablePlayerManager;
        this.customDiscs = customDiscs;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        NamespacedKey key = new NamespacedKey(plugin, "portable_player");
        if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            event.setCancelled(true);

            portablePlayerMenu.initializeItems(event.getPlayer());
            portablePlayerMenu.open(event.getPlayer());
            updateMenu(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isPortablePlayer(event.getItemDrop().getItemStack()) && portablePlayerManager.isPlaying(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PortablePlayerMenu)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();


        if (clickedItem != null && customDiscCommand.isCustomDisc(clickedItem)) {
            assert VoicePlugin.voicechatServerApi != null;

            portablePlayerManager.stopAudio(player);

            ItemMeta discMeta = clickedItem.getItemMeta();
            String soundFileName = discMeta.getPersistentDataContainer().get(new NamespacedKey(customDiscs, "customdisc"), PersistentDataType.STRING);
            if (soundFileName == null) {
                player.sendMessage("File not found");
                return;
            }
            Path soundFilePath = Path.of(customDiscs.getDataFolder().getPath(), "musicdata", soundFileName);

            float range = 32;
            Component songNameComponent = Objects.requireNonNull(clickedItem.getItemMeta().lore()).getFirst().asComponent();
            String songName = PlainTextComponentSerializer.plainText().serialize(songNameComponent);
            Component customActionBarSongPlaying = LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.NOW_PLAYING.toString().replace("%song_name%", songName));


            portablePlayerManager.playAudio(VoicePlugin.voicechatServerApi, soundFilePath, player, customActionBarSongPlaying, range);
            player.closeInventory();
            return;
        }

        if (event.getRawSlot() == 49) {
            portablePlayerManager.stopAudio(player);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player.getInventory().getHolder() instanceof PortablePlayerMenu)) return;

        portablePlayerMenu.updateItems(player);
    }

    private boolean isPortablePlayer(ItemStack item) {
        if(!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "portable_player"));
    }

    private void updateMenu(Player player) {
        player.closeInventory();
        new PortablePlayerMenu(customDiscCommand, portablePlayerManager).open(player);
    }

}