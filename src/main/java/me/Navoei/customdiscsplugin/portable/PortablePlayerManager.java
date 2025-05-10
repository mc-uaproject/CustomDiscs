package me.Navoei.customdiscsplugin.portable;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import me.Navoei.customdiscsplugin.CustomDiscs;
import me.Navoei.customdiscsplugin.VoicePlugin;
import me.Navoei.customdiscsplugin.language.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jflac.sound.spi.Flac2PcmAudioInputStream;
import org.jflac.sound.spi.FlacAudioFileReader;

import javax.annotation.Nullable;
import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class PortablePlayerManager {

    CustomDiscs plugin = CustomDiscs.getInstance();
    private final Map<UUID, PlayerReference> playerMap;
    private final Map<UUID, BukkitTask> positionUpdateTasks;
    private final ExecutorService executorService;
    private static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    public PortablePlayerManager() {
        this.playerMap = new ConcurrentHashMap<>();
        this.positionUpdateTasks = new ConcurrentHashMap<>();
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "PortableAudioPlayerThread");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void playAudio(VoicechatServerApi api, Path soundFilePath, Player player, Component actionbarComponent, float range) {
        UUID playerId = player.getUniqueId();

        if (playerMap.containsKey(playerId)) {
            stopAudio(player);
        }

        Location playerLoc = player.getLocation();
        LocationalAudioChannel audioChannel = api.createLocationalAudioChannel(
                UUID.randomUUID(),
                api.fromServerLevel(player.getWorld()),
                api.createPosition(playerLoc.getX(), playerLoc.getY(), playerLoc.getZ())
        );

        if (audioChannel == null) return;

        audioChannel.setCategory(VoicePlugin.MUSIC_DISC_CATEGORY);
        audioChannel.setDistance(range);
        player.sendActionBar(actionbarComponent);

        AtomicBoolean stopped = new AtomicBoolean(false);
        AtomicReference<AudioPlayer> playerRef = new AtomicReference<>();

        BukkitTask positionTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || stopped.get()) {
                stopAudio(player);
                return;
            }
            if (player.isDead()) {
                stopAudio(player);
                return;
            }
            Location newLoc = player.getLocation();
            audioChannel.updateLocation(api.createPosition(newLoc.getX(), newLoc.getY(), newLoc.getZ()));
        }, 0L, 1L);

        positionUpdateTasks.put(playerId, positionTask);

        executorService.execute(() -> {
            AudioInputStream inputStream = null;
            try {
                inputStream = getAudioInputStream(soundFilePath, FORMAT);

                AudioInputStream finalInputStream = inputStream;
                AudioPlayer audioPlayer = api.createAudioPlayer(audioChannel, api.createEncoder(), () -> {
                    try {
                        return readSoundFile(finalInputStream);
                    } catch (Exception e) {
                        if (!stopped.get()) {
                            plugin.getLogger().log(Level.SEVERE, "Помилка читання аудіо", e);
                        }
                        return null;
                    }
                });

                audioPlayer.startPlaying();
                playerRef.set(audioPlayer);

                AudioInputStream finalInputStream1 = inputStream;
                PlayerReference reference = new PlayerReference(
                        () -> {
                            stopped.set(true);
                            audioPlayer.stopPlaying();
                            positionTask.cancel();
                            try {
                                finalInputStream1.close();
                            } catch (IOException e) {
                                plugin.getLogger().log(Level.WARNING, "InputStreamClose error", e);
                            }
                        },
                        playerRef,
                        soundFilePath,
                        audioChannel,
                        inputStream
                );

                playerMap.put(playerId, reference);

            } catch (Exception e) {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.PREFIX + Lang.DOWNLOAD_ERROR.toString()));
                plugin.getLogger().log(Level.SEVERE, "Audio initialization error", e);
                positionTask.cancel();
            }
        });
    }

    @Nullable
    private AudioPlayer playChannel(VoicechatServerApi api, AudioChannel audioChannel, Player player, AudioInputStream inputStream) {
        AudioPlayer audioPlayer = api.createAudioPlayer(audioChannel, api.createEncoder(), () -> {
            try {
                return readSoundFile(inputStream);
            } catch (Exception e) {
                plugin.getLogger().info("Error Occurred For Player: " + player.getName());
                TextComponent textComponent = Component.text("An error has occurred while trying to play this disc.").color(NamedTextColor.RED);
                player.sendMessage(textComponent);
                e.printStackTrace();
                return null;
            }
        });
        audioPlayer.startPlaying();
        return audioPlayer;
    }

    public void stopAudio(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerReference playerRef = playerMap.get(playerId);
        if (playerRef != null) {
            playerRef.onStop.stop();
            BukkitTask task = positionUpdateTasks.remove(playerId);
            if (task != null) {
                task.cancel();
            }
            try {
                if (playerRef.inputStream() != null) {
                    playerRef.inputStream().close();
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Помилка закриття потоку", e);
            }
            playerMap.remove(playerId);
        }
    }

    public boolean isPlaying(Player player) {
        return playerMap.containsKey(player.getUniqueId());
    }

    private static byte[] getAudioPacket(AudioInputStream inputStream) throws IOException {
        byte[] audioPacket = inputStreamToPackets(inputStream);
        if (audioPacket == null) {
            return null;
        }
        return adjustVolume(audioPacket, CustomDiscs.getInstance().musicDiscVolume);
    }

    private static byte[] inputStreamToPackets(AudioInputStream inputStream) throws IOException {
        int FRAME_SIZE_BYTES = 1920;
        byte[] buffer = new byte[FRAME_SIZE_BYTES];  // Buffer to hold 960 bytes of audio data
        int bytesRead = inputStream.read(buffer);
        // If fewer than 960 bytes are read, pad with zeros
        if (bytesRead == -1) return null;
        if (bytesRead < FRAME_SIZE_BYTES) {
            for (int i = bytesRead; i < FRAME_SIZE_BYTES; i++) {
                buffer[i] = 0;  // Pad with zero
            }
        }
        return buffer;
    }

    public AudioInputStream getAudioInputStream(Path file, AudioFormat audioFormat) throws UnsupportedAudioFileException, IOException {
        AudioInputStream finalInputStream = null;
        if (getFileExtension(file.toFile().toString()).equals("wav")) {
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(file.toFile());
            finalInputStream = AudioSystem.getAudioInputStream(audioFormat, inputStream);
        } else if (getFileExtension(file.toFile().toString()).equals("mp3")) {

            AudioInputStream inputStream = new MpegAudioFileReader().getAudioInputStream(file.toFile());
            AudioFormat baseFormat = inputStream.getFormat();
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getFrameRate(), false);
            AudioInputStream convertedInputStream = new MpegFormatConversionProvider().getAudioInputStream(decodedFormat, inputStream);
            finalInputStream = AudioSystem.getAudioInputStream(audioFormat, convertedInputStream);

        } else if (getFileExtension(file.toFile().toString()).equals("flac")) {
            AudioInputStream inputStream = new FlacAudioFileReader().getAudioInputStream(file.toFile());
            AudioFormat baseFormat = inputStream.getFormat();
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getFrameRate(), false);
            AudioInputStream convertedInputStream = new Flac2PcmAudioInputStream(inputStream, decodedFormat, inputStream.getFrameLength());
            finalInputStream = AudioSystem.getAudioInputStream(audioFormat, convertedInputStream);
        }
        return finalInputStream;
    }

    public static short[] readSoundFile(AudioInputStream inputStream) throws IOException {
        byte[] audioPacket = getAudioPacket(inputStream);
        if (audioPacket == null) return null;
        return VoicePlugin.voicechatApi.getAudioConverter().bytesToShorts(audioPacket);
    }



    private static byte[] adjustVolume(byte[] audioSamples, double volume) {

        if (audioSamples == null) return null;

        if (volume > 1d || volume < 0d) {
            CustomDiscs.getInstance().getLogger().info("Error: The volume must be between 0 and 1 in the config!");
            return null;
        }

        byte[] array = new byte[audioSamples.length];
        for (int i = 0; i < array.length; i+=2) {
            // convert byte pair to int
            short buf1 = audioSamples[i+1];
            short buf2 = audioSamples[i];

            buf1 = (short) ((buf1 & 0xff) << 8);
            buf2 = (short) (buf2 & 0xff);

            short res= (short) (buf1 | buf2);
            res = (short) (res * volume);

            // convert back
            array[i] = (byte) res;
            array[i+1] = (byte) (res >> 8);

        }
        return array;
    }

    private static String getFileExtension(String s) {
        int index = s.lastIndexOf(".");
        if (index > 0) {
            return s.substring(index + 1);
        } else {
            return "";
        }
    }

    private static PortablePlayerManager instance;

    public static PortablePlayerManager instance() {
        if (instance == null) {
            instance = new PortablePlayerManager();
        }
        return instance;
    }

    private interface Stoppable {
        void stop();
    }

    public record PlayerReference(
            Stoppable onStop,
            AtomicReference<AudioPlayer> player,
            Path soundFilePath,
            LocationalAudioChannel channel,
            @Nullable AudioInputStream inputStream
    ) {}
}