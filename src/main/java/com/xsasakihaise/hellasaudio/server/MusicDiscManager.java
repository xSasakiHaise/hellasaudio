package com.xsasakihaise.hellasaudio.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xsasakihaise.hellasaudio.HellasAudio;
import com.xsasakihaise.hellasaudio.item.CustomDiscItem;
import com.xsasakihaise.hellasaudio.network.NetworkHandler;
import com.xsasakihaise.hellasaudio.network.message.DiscPlaybackMessage;
import com.xsasakihaise.hellasaudio.server.permission.PermissionHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks custom discs on the server and exposes helper methods for commands and network listeners.
 */
@Mod.EventBusSubscriber(modid = HellasAudio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MusicDiscManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final MusicDiscManager INSTANCE = new MusicDiscManager();
    private static final String METADATA_FILE = "discs.json";

    private final Map<String, DiscMetadata> discs = new ConcurrentHashMap<>();
    private Path storageRoot;
    private Path metadataFile;
    private MinecraftServer server;

    private MusicDiscManager() {
    }

    /**
     * @return singleton instance responsible for managing disc metadata and files.
     */
    public static MusicDiscManager getInstance() {
        return INSTANCE;
    }

    /**
     * Ensures the base HellasAudio directory exists prior to server start. This avoids late directory creation warnings
     * once uploads begin streaming in.
     */
    public static void prepareGlobalStorage() {
        try {
            Files.createDirectories(FMLPaths.GAMEDIR.get().resolve(HellasAudio.MOD_ID));
        } catch (IOException e) {
            HellasAudio.LOGGER.warn("Failed to pre-create global HellasAudio directories", e);
        }
    }

    @SubscribeEvent
    public static void handleServerStarting(FMLServerStartingEvent event) {
        // Capture the live server reference and load metadata as soon as possible so commands are ready at spawn.
        INSTANCE.server = event.getServer();
        INSTANCE.setupStorage();
        INSTANCE.loadMetadata();
    }

    @SubscribeEvent
    public static void handleServerStopping(FMLServerStoppingEvent event) {
        // Persist the final state before the server fully tears down.
        INSTANCE.persistMetadata();
        INSTANCE.server = null;
        INSTANCE.discs.clear();
    }

    private void setupStorage() {
        Path root = FMLPaths.GAMEDIR.get().resolve(HellasAudio.MOD_ID).resolve("discs");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            HellasAudio.LOGGER.error("Failed to create disc storage directory", e);
        }
        storageRoot = root;
        metadataFile = storageRoot.resolve(METADATA_FILE);
    }

    /**
     * Reads the persisted JSON file from disk and reconstructs in-memory metadata entries. Missing files simply result
     * in an empty library.
     */
    private void loadMetadata() {
        discs.clear();
        if (metadataFile == null || !Files.exists(metadataFile)) {
            return;
        }

        try (java.io.Reader reader = Files.newBufferedReader(metadataFile, StandardCharsets.UTF_8)) {
            JsonObject object = GSON.fromJson(reader, JsonObject.class);
            JsonArray array = object != null && object.has("discs") ? object.getAsJsonArray("discs") : new JsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject discObject = element.getAsJsonObject();
                String id = discObject.get("id").getAsString();
                String displayName = discObject.has("displayName")
                        ? sanitizeDisplayName(discObject.get("displayName").getAsString())
                        : createDefaultDisplayName(id);
                UUID uploader = discObject.has("uploader") ? UUID.fromString(discObject.get("uploader").getAsString()) : Util.NIL_UUID;
                Instant uploadedAt = discObject.has("uploadedAt") ? Instant.ofEpochMilli(discObject.get("uploadedAt").getAsLong()) : Instant.now();
                discs.put(id, new DiscMetadata(id, displayName, uploader, uploadedAt));
            }
        } catch (Exception e) {
            HellasAudio.LOGGER.error("Failed to load HellasAudio disc metadata", e);
        }
    }

    /**
     * Serializes the current metadata collection to disk so server restarts retain player uploads.
     */
    private void persistMetadata() {
        if (metadataFile == null) {
            return;
        }

        JsonArray array = new JsonArray();
        for (DiscMetadata metadata : discs.values()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", metadata.discId);
            object.addProperty("displayName", metadata.displayName);
            object.addProperty("uploader", metadata.uploader.toString());
            object.addProperty("uploadedAt", metadata.uploadedAt.toEpochMilli());
            array.add(object);
        }

        JsonObject root = new JsonObject();
        root.add("discs", array);

        try (BufferedWriter writer = Files.newBufferedWriter(metadataFile, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            HellasAudio.LOGGER.error("Failed to write HellasAudio disc metadata", e);
        }
    }

    /**
     * @return read-only collection of discs sorted by display name for presentation in commands or GUIs.
     */
    public Collection<DiscMetadata> getAllDiscs() {
        ArrayList<DiscMetadata> metadata = new ArrayList<>(discs.values());
        metadata.sort(Comparator.comparing(DiscMetadata::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return Collections.unmodifiableCollection(metadata);
    }

    /**
     * @return read-only collection of known disc identifiers sorted alphabetically.
     */
    public Collection<String> getKnownDiscIds() {
        ArrayList<String> ids = new ArrayList<>(discs.keySet());
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return Collections.unmodifiableCollection(ids);
    }

    /**
     * Looks up metadata for a specific disc.
     */
    public Optional<DiscMetadata> getMetadata(String discId) {
        return Optional.ofNullable(discs.get(discId));
    }

    /**
     * Builds an item stack representing the requested disc so staff can hand it out through commands.
     */
    public ItemStack createDiscItem(String discId) {
        DiscMetadata metadata = discs.get(discId);
        if (metadata == null) {
            return ItemStack.EMPTY;
        }
        return CustomDiscItem.createForDisc(discId, metadata.getDisplayName());
    }

    /**
     * Performs the validation and file write for a client upload request. Players receive descriptive feedback when a
     * rule prevents their upload.
     */
    public void handleUpload(ServerPlayerEntity player, String discId, byte[] data) {
        if (storageRoot == null) {
            player.sendMessage(new StringTextComponent("Server is not ready to handle uploads."), Util.NIL_UUID);
            return;
        }

        if (!PermissionHandler.canUpload(player)) {
            player.sendMessage(new StringTextComponent(TextFormatting.RED + "You do not have permission to upload HellasAudio discs (" + PermissionHandler.UPLOAD_PERMISSION + ")."), Util.NIL_UUID);
            return;
        }

        String sanitizedId = sanitizeIdentifier(discId);
        if (sanitizedId.isEmpty()) {
            player.sendMessage(new StringTextComponent("Upload failed: invalid disc identifier."), Util.NIL_UUID);
            return;
        }

        if (data.length == 0) {
            player.sendMessage(new StringTextComponent("Upload failed: MP3 file was empty."), Util.NIL_UUID);
            return;
        }

        if (data.length > HellasAudio.MAX_DISC_SIZE_BYTES) {
            player.sendMessage(new StringTextComponent("Upload failed: file exceeds server limit of " + (HellasAudio.MAX_DISC_SIZE_BYTES / (1024 * 1024)) + " MiB."), Util.NIL_UUID);
            return;
        }

        if (!looksLikeMp3(data)) {
            player.sendMessage(new StringTextComponent("Upload failed: file does not appear to be an MP3."), Util.NIL_UUID);
            return;
        }

        Path destination = storageRoot.resolve(sanitizedId + ".mp3");
        try {
            Files.write(destination, data);
            DiscMetadata metadata = new DiscMetadata(sanitizedId, createDefaultDisplayName(sanitizedId), player.getUUID(), Instant.now());
            discs.put(sanitizedId, metadata);
            persistMetadata();
            player.sendMessage(new StringTextComponent(TextFormatting.GREEN + "Uploaded disc '" + metadata.getDisplayName() + "' (" + sanitizedId + ")."), Util.NIL_UUID);
            HellasAudio.LOGGER.info("Player {} uploaded disc '{}' ({} bytes)", player.getScoreboardName(), sanitizedId, data.length);
        } catch (IOException e) {
            HellasAudio.LOGGER.error("Failed to store uploaded disc", e);
            player.sendMessage(new StringTextComponent("Upload failed: " + e.getMessage()), Util.NIL_UUID);
        }
    }

    /**
     * Broadcasts a disc to either the provided targets or the entire server if the collection is {@code null} or empty.
     * The file is streamed into a {@link DiscPlaybackMessage} for each listener.
     */
    public void playDisc(CommandSource source, String discId, @Nullable Collection<ServerPlayerEntity> targets) {
        DiscMetadata metadata = discs.get(discId);
        if (metadata == null) {
            source.sendFailure(new StringTextComponent("Unknown disc id: " + discId));
            return;
        }

        if (storageRoot == null) {
            source.sendFailure(new StringTextComponent("Server storage has not been initialized."));
            return;
        }

        if (server == null) {
            source.sendFailure(new StringTextComponent("Server reference unavailable; cannot broadcast."));
            return;
        }

        Path file = storageRoot.resolve(discId + ".mp3");
        if (!Files.exists(file)) {
            source.sendFailure(new StringTextComponent("Stored disc data missing for '" + discId + "'."));
            return;
        }

        try {
            byte[] payload = Files.readAllBytes(file);
            Iterable<ServerPlayerEntity> listeners = targets != null && !targets.isEmpty()
                    ? targets
                    : server.getPlayerList().getPlayers();
            NetworkHandler.broadcastToPlayers(new DiscPlaybackMessage(discId, payload), listeners);
            source.sendSuccess(new StringTextComponent("Broadcasted disc '" + metadata.getDisplayName() + "' (" + discId + ") to " + countTargets(listeners) + " player(s)."), true);
        } catch (IOException e) {
            HellasAudio.LOGGER.error("Failed to read disc data for '{}'", discId, e);
            source.sendFailure(new StringTextComponent("Failed to read stored disc: " + e.getMessage()));
        }
    }

    /**
     * Updates the friendly display name for a disc.
     */
    public boolean renameDisc(String discId, String newDisplayName) {
        DiscMetadata existing = discs.get(discId);
        if (existing == null) {
            return false;
        }

        String sanitized = sanitizeDisplayName(newDisplayName);
        if (sanitized.isEmpty()) {
            return false;
        }

        discs.put(discId, existing.withDisplayName(sanitized));
        persistMetadata();
        return true;
    }

    /**
     * Deletes both metadata and the stored MP3 file, if present.
     */
    public boolean removeDisc(String discId) {
        DiscMetadata metadata = discs.remove(discId);
        if (metadata == null) {
            return false;
        }

        if (storageRoot != null) {
            Path file = storageRoot.resolve(discId + ".mp3");
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                HellasAudio.LOGGER.warn("Failed to delete mp3 file for disc '{}'", discId, e);
            }
        }
        persistMetadata();
        return true;
    }

    private int countTargets(Iterable<ServerPlayerEntity> players) {
        int count = 0;
        for (ServerPlayerEntity ignored : players) {
            count++;
        }
        return count;
    }

    private String sanitizeIdentifier(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (char character : trimmed.toCharArray()) {
            if (Character.isLetterOrDigit(character) || character == '_' || character == '-') {
                builder.append(Character.toLowerCase(character));
            }
        }
        return builder.toString();
    }

    private String sanitizeDisplayName(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        if (trimmed.length() > 80) {
            trimmed = trimmed.substring(0, 80);
        }
        return trimmed;
    }

    private String createDefaultDisplayName(String discId) {
        String base = discId.replace('_', ' ').replace('-', ' ');
        String[] parts = base.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        if (builder.length() == 0) {
            return discId;
        }
        return builder.toString();
    }

    private boolean looksLikeMp3(byte[] data) {
        if (data.length < 3) {
            return false;
        }
        // Basic heuristic: either ID3 tag or frame sync header.
        if (data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
            return true;
        }
        int b0 = data[0] & 0xFF;
        int b1 = data[1] & 0xE0;
        return b0 == 0xFF && b1 == 0xE0;
    }

    /**
     * Immutable snapshot of a disc entry that records the uploader and creation time alongside display data.
     */
    public static final class DiscMetadata {
        private final String discId;
        private final String displayName;
        private final UUID uploader;
        private final Instant uploadedAt;

        private DiscMetadata(String discId, String displayName, UUID uploader, Instant uploadedAt) {
            this.discId = discId;
            this.displayName = displayName;
            this.uploader = uploader;
            this.uploadedAt = uploadedAt;
        }

        /**
         * @return globally unique identifier players refer to when requesting playback.
         */
        public String getDiscId() {
            return discId;
        }

        /**
         * @return player-facing name shown in tooltips and command listings.
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * @return UUID of the player who uploaded the disc.
         */
        public UUID getUploader() {
            return uploader;
        }

        /**
         * @return timestamp marking when the file was stored on the server.
         */
        public Instant getUploadedAt() {
            return uploadedAt;
        }

        private DiscMetadata withDisplayName(String newDisplayName) {
            return new DiscMetadata(discId, newDisplayName, uploader, uploadedAt);
        }
    }
}
