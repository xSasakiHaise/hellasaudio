package com.xsasakihaise.hellasaudio;

import com.xsasakihaise.hellascontrol.api.CoreCheck;
import com.xsasakihaise.hellasaudio.client.HellasAudioClient;
import com.xsasakihaise.hellasaudio.network.NetworkHandler;
import com.xsasakihaise.hellasaudio.server.MusicDiscManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Primary mod class for HellasAudio. This is the entry point that Forge uses to initialize the mod.
 * It intentionally provides empty setup hooks that can be filled in as development continues.
 */
@Mod(HellasAudio.MOD_ID)
public class HellasAudio {
    public static final String MOD_ID = "hellasaudio";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final int MAX_DISC_SIZE_BYTES = 50 * 1024 * 1024; // 50 MiB hard limit for uploads

    /**
     * Registers lifecycle listeners and kicks off shared registries. Forge invokes this constructor exactly once when
     * the mod is loaded, making it a good place to wire common callbacks.
     */
    public HellasAudio() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onClientSetup);

        ModItems.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("HellasAudio mod initialized.");
    }

    /**
     * Performs server-agnostic initialization such as entitlement checks and network setup. The heavy lifting is
     * deferred to {@link FMLCommonSetupEvent#enqueueWork(Runnable)} so that registries stay thread-safe.
     */
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        CoreCheck.verifyCoreLoaded();

        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            CoreCheck.verifyEntitled("hellasaudio");
        }

        if (!ModList.get().isLoaded("hellascontrol")) {
            LOGGER.warn("HellasControl not present; skipping HellasAudio registration due to licensing requirements.");
            return;
        }

        event.enqueueWork(() -> {
            NetworkHandler.init();
            MusicDiscManager.prepareGlobalStorage();
        });
    }

    /**
     * Creates any required directories and client-side helpers before the player reaches the main menu.
     */
    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(HellasAudioClient::initializeClient);
    }
}
