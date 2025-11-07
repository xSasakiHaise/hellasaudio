package com.xsasakihaise.hellasaudio.client;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xsasakihaise.hellasaudio.HellasAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Lightweight parser that turns "/hellas audio upload" chat messages into client-side uploads. This keeps the network
 * payloads off the public chat channel while still providing an intuitive user experience.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = HellasAudio.MOD_ID, value = Dist.CLIENT)
public final class ClientCommandHandler {
    private ClientCommandHandler() {
    }

    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        String message = event.getMessage();
        UploadInvocation invocation = parseUploadInvocation(message);
        if (invocation == null) {
            return;
        }

        event.setCanceled(true);
        if (invocation.discId == null || invocation.discId.isEmpty() || invocation.path == null || invocation.path.isEmpty()) {
            notifyUsage();
            return;
        }

        try {
            HellasAudioClient.uploadDiscFromClient(invocation.discId, invocation.path);
        } catch (CommandSyntaxException exception) {
            notifyPlayer(exception.getMessage());
        }
    }

    private static void notifyUsage() {
        notifyPlayer("Usage: /hellas audio upload <disc_id> <path_to_mp3>. Place MP3 files in "
                + HellasAudioClient.getClientUploadRoot().toAbsolutePath()
                + " or provide an absolute path.");
    }

    private static void notifyPlayer(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendMessage(new StringTextComponent(message), Util.NIL_UUID);
        }
    }

    @Nullable
    private static UploadInvocation parseUploadInvocation(@Nullable String rawMessage) {
        if (rawMessage == null) {
            return null;
        }

        String trimmed = rawMessage.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.startsWith("/hellas")) {
            String remainder = trimmed.substring("/hellas".length()).trim();
            if (!remainder.toLowerCase(Locale.ROOT).startsWith("audio")) {
                return null;
            }

            remainder = remainder.substring("audio".length()).trim();
            if (remainder.toLowerCase(Locale.ROOT).startsWith("upload")) {
                return extractArguments(remainder.substring("upload".length()).trim());
            }
        }

        return null;
    }

    private static UploadInvocation extractArguments(String argumentSection) {
        if (argumentSection.isEmpty()) {
            return new UploadInvocation("", "");
        }

        String[] split = argumentSection.split(" ", 2);
        if (split.length < 2) {
            return new UploadInvocation(split[0], "");
        }
        return new UploadInvocation(split[0], split[1].trim());
    }

    private static final class UploadInvocation {
        private final String discId;
        private final String path;

        private UploadInvocation(String discId, String path) {
            this.discId = discId;
            this.path = path;
        }
    }
}
