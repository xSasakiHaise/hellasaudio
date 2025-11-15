package com.xsasakihaise.hellasaudio;

import com.xsasakihaise.hellasaudio.item.CustomDiscItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Central registry for HellasAudio mod items.
 */
public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HellasAudio.MOD_ID);

    public static final RegistryObject<CustomDiscItem> CUSTOM_DISC = ITEMS.register("custom_disc",
            () -> new CustomDiscItem(new Item.Properties().tab(ItemGroup.TAB_MISC).stacksTo(1)));

    private ModItems() {
    }

    /**
     * Hooks the mod's deferred register into the provided event bus. Must be called during mod construction to ensure
     * discs appear in the registry at the correct time.
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
