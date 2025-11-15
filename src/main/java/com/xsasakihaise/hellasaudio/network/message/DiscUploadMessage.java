package com.xsasakihaise.hellasaudio.network.message;

import com.xsasakihaise.hellasaudio.server.MusicDiscManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from clients to the server when a player uploads an MP3 that should be stored as a music disc.
 */
public class DiscUploadMessage {
    private final String discId;
    private final byte[] payload;

    public DiscUploadMessage(String discId, byte[] payload) {
        this.discId = discId;
        this.payload = payload;
    }

    public String getDiscId() {
        return discId;
    }

    public byte[] getPayload() {
        return payload;
    }

    /**
     * Serializes the message into the binary packet buffer. The payload can be relatively large so we always prefix it
     * with a length field to guard against buffer under-reads.
     */
    public static void encode(DiscUploadMessage message, PacketBuffer buffer) {
        buffer.writeUtf(message.discId, 64);
        buffer.writeVarInt(message.payload.length);
        buffer.writeByteArray(message.payload);
    }

    /**
     * Deserializes packet data received from the network into an immutable message instance.
     */
    public static DiscUploadMessage decode(PacketBuffer buffer) {
        String discId = buffer.readUtf(64);
        int length = buffer.readVarInt();
        byte[] payload = buffer.readByteArray(length);
        return new DiscUploadMessage(discId, payload);
    }

    /**
     * Invoked on the server after the client sends an upload request. The heavy processing is delegated to
     * {@link MusicDiscManager} so permissions and storage live in a single location.
     */
    public static void handle(DiscUploadMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayerEntity sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender != null) {
                MusicDiscManager.getInstance().handleUpload(sender, message.discId, message.payload);
            }
        });
        context.setPacketHandled(true);
    }
}
