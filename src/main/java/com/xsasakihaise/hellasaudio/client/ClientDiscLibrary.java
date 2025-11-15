package com.xsasakihaise.hellasaudio.client;

import com.xsasakihaise.hellasaudio.HellasAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local cache of discs that were transmitted by the server. The actual decoding and playback of MP3 data is deferred to
 * whatever audio pipeline HellasControl exposes. For now the client simply logs and notifies the player when a new disc is
 * ready on disk so that the existing audio infrastructure can consume it.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientDiscLibrary {
    private static final ClientDiscLibrary INSTANCE = new ClientDiscLibrary();
    private final Map<String, Path> cachedDiscs = new ConcurrentHashMap<>();

    private ClientDiscLibrary() {
    }

    /**
     * @return singleton reference used across all client handlers.
     */
    public static ClientDiscLibrary getInstance() {
        return INSTANCE;
    }

    /**
     * Stores the latest MP3 path for a disc identifier and notifies the player that an external audio system can now
     * consume it.
     */
    public void queuePlayback(String discId, Path cachedFile) {
        cachedDiscs.put(discId, cachedFile);
        HellasAudio.LOGGER.info("Queued custom disc '{}' located at {}", discId, cachedFile);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendMessage(new StringTextComponent(TextFormatting.GRAY + "Cached disc '" + discId + "' at " + cachedFile), Util.NIL_UUID);
        }

        // Hook into the external audio system if/when it becomes available.
    }

    /**
     * @return live view of cached discs keyed by disc id.
     */
    public Map<String, Path> getCachedDiscs() {
        return cachedDiscs;
    }
}
