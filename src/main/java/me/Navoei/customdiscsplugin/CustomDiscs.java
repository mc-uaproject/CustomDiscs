package me.Navoei.customdiscsplugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.maxhenkel.voicechat.api.*;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import me.Navoei.customdiscsplugin.command.CustomDiscCommand;
import me.Navoei.customdiscsplugin.command.TokenDiscCommand;
import me.Navoei.customdiscsplugin.custom_items.PortablePlayerRecipe;
import me.Navoei.customdiscsplugin.event.JukeBox;
import me.Navoei.customdiscsplugin.language.Lang;
import me.Navoei.customdiscsplugin.listeners.HopperManager;
import me.Navoei.customdiscsplugin.listeners.PortablePlayerListener;
import me.Navoei.customdiscsplugin.utils.PortablePlayerManager;
import me.Navoei.customdiscsplugin.utils.PortablePlayerMenu;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Jukebox;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;

import java.io.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CustomDiscs extends JavaPlugin {
    static CustomDiscs instance;

    private LiteCommands<CommandSender> liteCommands;

    @Nullable
    private VoicePlugin voicechatPlugin;
    private Logger log;
    public static YamlConfiguration LANG;
    public static File LANG_FILE;
    public float musicDiscDistance;
    public float musicDiscMaxDistance;
    public float musicDiscVolume;
    private CustomDiscCommand customDiscCommand;
    private PortablePlayerManager portablePlayerManager;
    private PortablePlayerMenu portablePlayerMenu;

    @Override
    public void onLoad() {
        CustomDiscs.instance = this;
    }

    @Override
    public void onEnable() {
        this.customDiscCommand = new CustomDiscCommand(this);
        this.portablePlayerManager = PortablePlayerManager.instance();
        this.portablePlayerMenu = new PortablePlayerMenu(customDiscCommand, portablePlayerManager);

        this.liteCommands = LiteBukkitFactory.builder("CustomDiscs", this)
                .commands(
                        new TokenDiscCommand(this),
                        new CustomDiscCommand(this)
                )
                .build();

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);

        this.saveDefaultConfig();
        loadLang();

        File musicData = new File(this.getDataFolder(), "musicdata");
        if (!(musicData.exists())) {
            musicData.mkdirs();
        }

        if (service != null) {
            voicechatPlugin = new VoicePlugin();
            service.registerPlugin(voicechatPlugin);
            getLogger().info("Successfully registered CustomDiscs plugin");
        } else {
            getLogger().info("Failed to register CustomDiscs plugin");
        }
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_EVENT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();

                if (packet.getIntegers().read(0).toString().equals("1010")) {
                    Jukebox jukebox = (Jukebox) packet.getBlockPositionModifier().read(0).toLocation(event.getPlayer().getWorld()).getBlock().getState();

                    if (!jukebox.getRecord().hasItemMeta()) return;

                    if (jukebox.getRecord().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this.plugin, "customdisc"), PersistentDataType.STRING)) {
                        event.setCancelled(true);
                    }

                    //Start the jukebox state manager.
                    //This keeps the jukebox powered while custom song is playing,
                    //which perfectly emulates the vanilla behavior of discs.
                    JukeboxStateManager.start(jukebox);
                }
            }
        });

        getServer().getPluginManager().registerEvents(new JukeBox(), this);
        getServer().getPluginManager().registerEvents(new HopperManager(), this);
        getServer().getPluginManager().registerEvents(new PortablePlayerListener(this, customDiscCommand, portablePlayerMenu, portablePlayerManager, this), this);

        PortablePlayerRecipe recipe = new PortablePlayerRecipe(this);
        recipe.registerRecipes();

        PortablePlayerMenu portablePlayerMenu = new PortablePlayerMenu(customDiscCommand, portablePlayerManager);

        musicDiscDistance = getConfig().getInt("music-disc-distance");
        musicDiscMaxDistance = getConfig().getInt("music-disc-max-distance");
        musicDiscVolume = Float.parseFloat(Objects.requireNonNull(getConfig().getString("music-disc-volume")));



    }

    @Override
    public void onDisable() {
        if (this.liteCommands != null) {
            this.liteCommands.unregister();
        }
        if (voicechatPlugin != null) {
            getServer().getServicesManager().unregister(voicechatPlugin);
            getLogger().info("Successfully unregistered CustomDiscs plugin");
        }
    }

    public static CustomDiscs getInstance() {
        return instance;
    }

    /**
     * Load the lang.yml file.
     *
     * @return The lang.yml config.
     */
    public void loadLang() {
        File lang = new File(getDataFolder(), "lang.yml");
        if (!lang.exists()) {
            try {
                getDataFolder().mkdir();
                lang.createNewFile();
                InputStream defConfigStream = this.getResource("lang.yml");
                if (defConfigStream != null) {
                    copyInputStreamToFile(defConfigStream, lang);
                    YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(lang);
                    defConfig.save(lang);
                    Lang.setFile(defConfig);
                }
            } catch (IOException e) {
                e.printStackTrace(); // So they notice
                log.severe("Failed to create lang.yml for CustomDiscs.");
                log.severe("Now disabling...");
                this.setEnabled(false); // Without it loaded, we can't send them messages
            }
        }
        YamlConfiguration conf = YamlConfiguration.loadConfiguration(lang);
        for (Lang item : Lang.values()) {
            if (conf.getString(item.getPath()) == null) {
                conf.set(item.getPath(), item.getDefault());
            }
        }
        Lang.setFile(conf);
        LANG = conf;
        LANG_FILE = lang;
        try {
            conf.save(getLangFile());
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to save lang.yml for CustomDiscs");
            log.log(Level.WARNING, "Now disabling...");
            e.printStackTrace();
        }
    }

    /**
     * Gets the lang.yml config.
     *
     * @return The lang.yml config.
     */
    public YamlConfiguration getLang() {
        return LANG;
    }

    /**
     * Get the lang.yml file.
     *
     * @return The lang.yml file.
     */
    public File getLangFile() {
        return LANG_FILE;
    }

    public static void copyInputStreamToFile(InputStream input, File file) {

        try (OutputStream output = new FileOutputStream(file)) {
            input.transferTo(output);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }
}
