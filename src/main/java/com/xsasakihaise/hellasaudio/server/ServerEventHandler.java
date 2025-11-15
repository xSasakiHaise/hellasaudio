package com.xsasakihaise.hellasaudio.server;

import com.xsasakihaise.hellasaudio.HellasAudio;
import com.xsasakihaise.hellasaudio.server.command.MusicDiscCommands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hosts Forge level event subscriptions for server-only features.
 */
@Mod.EventBusSubscriber(modid = HellasAudio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerEventHandler {
    private ServerEventHandler() {
    }

    /**
     * Registers HellasAudio's Brigadier command tree during the standard Forge callback.
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MusicDiscCommands.register(event.getDispatcher());
    }
}
