package me.Navoei.customdiscsplugin;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jflac.sound.spi.Flac2PcmAudioInputStream;
import org.jflac.sound.spi.FlacAudioFileReader;

import javax.annotation.Nullable;
import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerManager {

    CustomDiscs plugin = CustomDiscs.getInstance();
    private final Map<UUID, PlayerReference> playerMap;
    private final ExecutorService executorService;
    private static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    public PlayerManager() {
        this.playerMap = new ConcurrentHashMap<>();
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "AudioPlayerThread");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void playAudio(VoicechatServerApi api, Path soundFilePath, Block block, Component actionbarComponent, float range) {
        UUID id = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());

        LocationalAudioChannel audioChannel = api.createLocationalAudioChannel(id, api.fromServerLevel(block.getWorld()), api.createPosition(block.getLocation().getX() + 0.5d, block.getLocation().getY() + 0.5d, block.getLocation().getZ() + 0.5d));

        if (audioChannel == null) return;

        audioChannel.setCategory(VoicePlugin.MUSIC_DISC_CATEGORY);
        audioChannel.setDistance(range);

        Collection<ServerPlayer> playersInRange = api.getPlayersInRange(api.fromServerLevel(block.getWorld()), audioChannel.getLocation(), range);

        for (ServerPlayer serverPlayer : playersInRange) {
            Player bukkitPlayer = (Player) serverPlayer.getPlayer();
            bukkitPlayer.sendActionBar(actionbarComponent);
        }

        AtomicBoolean stopped = new AtomicBoolean();
        AtomicReference<de.maxhenkel.voicechat.api.audiochannel.AudioPlayer> player = new AtomicReference<>();
        PlayerReference playerReference = new PlayerReference(() -> {
            synchronized (stopped) {
                stopped.set(true);
                de.maxhenkel.voicechat.api.audiochannel.AudioPlayer audioPlayer = player.get();
                if (audioPlayer != null) {
                    audioPlayer.stopPlaying();
                }
            }
        }, player, soundFilePath);

        playerMap.put(id, playerReference);

        executorService.execute(() -> {
            AudioPlayer audioPlayer = null;
            AudioInputStream inputStream = null;
            try {
                inputStream = getAudioInputStream(soundFilePath, FORMAT);
                audioPlayer = playChannel(api, audioChannel, block, inputStream, playersInRange);
            } catch (UnsupportedAudioFileException | IOException e) {
                throw new RuntimeException(e);
            }
            if (audioPlayer == null) {
                playerMap.remove(id);
                return;
            }
            AudioInputStream finalInputStream = inputStream;
            audioPlayer.setOnStopped(() -> {
                try {
                    finalInputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //plugin.getServer().getRegionScheduler().run(plugin, block.getLocation(), scheduledTask -> HopperManager.instance().discToHopper(block));
                if (playerMap.containsValue(playerReference)) {
                    playerMap.remove(id);
                }
            });
            synchronized (stopped) {
                if (!stopped.get()) {
                    player.set(audioPlayer);
                } else {
                    audioPlayer.stopPlaying();
                }
            }
        });

    }

    @Nullable
    private de.maxhenkel.voicechat.api.audiochannel.AudioPlayer playChannel(VoicechatServerApi api, AudioChannel audioChannel, Block block, AudioInputStream inputStream, Collection<ServerPlayer> playersInRange) throws UnsupportedAudioFileException, IOException {
        //short[] audio = readSoundFile(soundFilePath);
        AudioPlayer audioPlayer = api.createAudioPlayer(audioChannel, api.createEncoder(), () -> {
            try {
                return readSoundFile(inputStream);
            } catch (Exception e) {
                plugin.getLogger().info("Error Occurred At: " + block.getLocation());
                for (ServerPlayer serverPlayer : playersInRange) {
                    Player bukkitPlayer = (Player) serverPlayer.getPlayer();
                    TextComponent textComponent = Component.text("An error has occurred while trying to play this disc.").color(NamedTextColor.RED);
                    bukkitPlayer.sendMessage(textComponent);
                }
                e.printStackTrace();
                return null;
            }
        });
        audioPlayer.startPlaying();
        return audioPlayer;
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


    public void stopLocationalAudio(Location blockLocation) {
        UUID id = UUID.nameUUIDFromBytes(blockLocation.toString().getBytes());
        PlayerReference player = playerMap.get(id);
        if (player != null) {
            player.onStop.stop();
        }
    }

    //public static float getLengthSeconds(Path file) throws UnsupportedAudioFileException, IOException {
    //    short[] audio = readSoundFile(file);
    //    return (float) audio.length / FORMAT.getSampleRate();
    //}

    public boolean isAudioPlayerPlaying(Location blockLocation) {
        UUID id = UUID.nameUUIDFromBytes(blockLocation.toString().getBytes());
        return playerMap.containsKey(id);
    }

    private static String getFileExtension(String s) {
        int index = s.lastIndexOf(".");
        if (index > 0) {
            return s.substring(index + 1);
        } else {
            return "";
        }
    }

    private static PlayerManager instance;

    public static PlayerManager instance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    private interface Stoppable {
        void stop();
    }

    public record PlayerReference(Stoppable onStop,
                                  AtomicReference<de.maxhenkel.voicechat.api.audiochannel.AudioPlayer> player,
                                  Path soundFilePath) {
    }

}
