package com.xsasakihaise.hellasaudio.network;

import com.xsasakihaise.hellasaudio.HellasAudio;
import com.xsasakihaise.hellasaudio.network.message.DiscPlaybackMessage;
import com.xsasakihaise.hellasaudio.network.message.DiscUploadMessage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * Central location for registering and working with the mod's network channel.
 */
public final class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HellasAudio.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static boolean initialized;

    private NetworkHandler() {
    }

    /**
     * Registers all packet types that the mod uses. Needs to be invoked during common setup before any packets are
     * transmitted.
     */
    public static void init() {
        if (initialized) {
            return;
        }

        int index = 0;
        CHANNEL.registerMessage(index++, DiscUploadMessage.class, DiscUploadMessage::encode, DiscUploadMessage::decode, DiscUploadMessage::handle);
        CHANNEL.registerMessage(index++, DiscPlaybackMessage.class, DiscPlaybackMessage::encode, DiscPlaybackMessage::decode, DiscPlaybackMessage::handle);
        initialized = true;
    }

    /**
     * Convenience wrapper for pushing messages to the logical server.
     */
    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    /**
     * Sends a packet to a single player. Used for targeted playback requests.
     */
    public static void sendToPlayer(Object message, ServerPlayerEntity player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /**
     * Iterates over the provided collection and sends the packet to each player individually. Forge's higher-level
     * packet distributors do not support arbitrary subsets, so we keep this helper dedicated to our use case.
     */
    public static void broadcastToPlayers(Object message, Iterable<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            sendToPlayer(message, player);
        }
    }

    /**
     * @return the configured SimpleChannel for advanced integrations.
     */
    public static SimpleChannel getChannel() {
        return CHANNEL;
    }
}
