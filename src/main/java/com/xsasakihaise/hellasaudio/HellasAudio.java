package com.xsasakihaise.hellasaudio;

import com.xsasakihaise.hellascontrol.api.CoreCheck;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
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
    private static final Logger LOGGER = LogManager.getLogger();

    public HellasAudio() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onClientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("HellasAudio mod initialized.");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        CoreCheck.verifyCoreLoaded();

        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            CoreCheck.verifyEntitled("hellasaudio");
        }

        if (!ModList.get().isLoaded("hellascontrol")) {
            LOGGER.warn("HellasControl not present; skipping HellasAudio registration due to licensing requirements.");
            return;
        }

        // Placeholder for common setup logic. Add registration calls here once HellasControl is verified.
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        // Placeholder for client-specific setup logic.
    }
}
