package com.xsasakihaise.hellasaudio.network.message;

import com.xsasakihaise.hellasaudio.client.HellasAudioClient;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from the server to clients instructing them to play a specific custom disc.
 */
public class DiscPlaybackMessage {
    private final String discId;
    private final byte[] payload;

    public DiscPlaybackMessage(String discId, byte[] payload) {
        this.discId = discId;
        this.payload = payload;
    }

    /**
     * Serializes the playback identifier and bytes into the buffer so clients can reconstruct a cache file.
     */
    public static void encode(DiscPlaybackMessage message, PacketBuffer buffer) {
        buffer.writeUtf(message.discId, 64);
        buffer.writeVarInt(message.payload.length);
        buffer.writeByteArray(message.payload);
    }

    /**
     * Creates a new message from the raw network buffer sent by the server.
     */
    public static DiscPlaybackMessage decode(PacketBuffer buffer) {
        String discId = buffer.readUtf(64);
        int length = buffer.readVarInt();
        byte[] payload = buffer.readByteArray(length);
        return new DiscPlaybackMessage(discId, payload);
    }

    /**
     * Handles the playback command by deferring to the {@link HellasAudioClient} singleton on the render thread.
     */
    public static void handle(DiscPlaybackMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> HellasAudioClient.handleServerPlayback(message.discId, message.payload));
        context.setPacketHandled(true);
    }
}
