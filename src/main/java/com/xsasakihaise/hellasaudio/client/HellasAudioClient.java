package com.xsasakihaise.hellasaudio.client;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.xsasakihaise.hellasaudio.HellasAudio;
import com.xsasakihaise.hellasaudio.network.NetworkHandler;
import com.xsasakihaise.hellasaudio.network.message.DiscUploadMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Client side helpers for upload commands and receiving playback events from the server.
 */
@OnlyIn(Dist.CLIENT)
public final class HellasAudioClient {
    private static final Path CLIENT_ROOT = FMLPaths.GAMEDIR.get().resolve(HellasAudio.MOD_ID);
    private static final Path CLIENT_UPLOAD_ROOT = CLIENT_ROOT.resolve("uploads");
    private static final Path CLIENT_CACHE_ROOT = CLIENT_ROOT.resolve("cache");

    private HellasAudioClient() {
    }

    public static void initializeClient() {
        try {
            Files.createDirectories(CLIENT_UPLOAD_ROOT);
            Files.createDirectories(CLIENT_CACHE_ROOT);
        } catch (IOException e) {
            HellasAudio.LOGGER.error("Unable to create HellasAudio client directories", e);
        }
    }

    public static void uploadDiscFromClient(String discId, String pathArgument) throws CommandSyntaxException {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.player == null) {
            throw new SimpleCommandExceptionType(new StringTextComponent("Cannot upload discs while not connected to a server.")).create();
        }

        Path resolved = resolveUploadPath(pathArgument);
        if (!Files.exists(resolved)) {
            minecraft.player.sendMessage(new StringTextComponent("Upload failed: file not found - " + resolved
                    + ". Place the MP3 inside " + CLIENT_UPLOAD_ROOT.toAbsolutePath() + " or provide an absolute path."), Util.NIL_UUID);
            return;
        }

        if (!resolved.toString().toLowerCase(Locale.ROOT).endsWith(".mp3")) {
            minecraft.player.sendMessage(new StringTextComponent("Upload failed: only MP3 files are supported."), Util.NIL_UUID);
            return;
        }

        try {
            long size = Files.size(resolved);
            if (size > HellasAudio.MAX_DISC_SIZE_BYTES) {
                minecraft.player.sendMessage(new StringTextComponent("Upload failed: file is larger than " + (HellasAudio.MAX_DISC_SIZE_BYTES / (1024 * 1024)) + " MiB."), Util.NIL_UUID);
                return;
            }

            byte[] data = Files.readAllBytes(resolved);
            NetworkHandler.sendToServer(new DiscUploadMessage(discId, data));
            minecraft.player.sendMessage(new StringTextComponent("Upload request sent for disc '" + discId + "'."), Util.NIL_UUID);
        } catch (IOException exception) {
            minecraft.player.sendMessage(new StringTextComponent("Upload failed: " + exception.getMessage()), Util.NIL_UUID);
        }
    }

    public static void handleServerPlayback(String discId, byte[] payload) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            Files.createDirectories(CLIENT_CACHE_ROOT);
            Path cachedFile = CLIENT_CACHE_ROOT.resolve(discId + "-" + DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + ".mp3");
            Files.write(cachedFile, payload);
            if (minecraft.player != null) {
                minecraft.player.sendMessage(new StringTextComponent("Now playing custom disc '" + discId + "' from server."), Util.NIL_UUID);
            }
            ClientDiscLibrary.getInstance().queuePlayback(discId, cachedFile);
        } catch (IOException e) {
            HellasAudio.LOGGER.error("Failed to cache disc '{}' locally", discId, e);
            if (minecraft.player != null) {
                minecraft.player.sendMessage(new StringTextComponent("Failed to cache disc '" + discId + "': " + e.getMessage()), Util.NIL_UUID);
            }
        }
    }

    public static Path getClientUploadRoot() {
        return CLIENT_UPLOAD_ROOT;
    }

    private static Path resolveUploadPath(String argument) {
        Path provided = Paths.get(argument);
        if (!provided.isAbsolute()) {
            provided = CLIENT_UPLOAD_ROOT.resolve(argument).normalize();
        }
        return provided;
    }
}
