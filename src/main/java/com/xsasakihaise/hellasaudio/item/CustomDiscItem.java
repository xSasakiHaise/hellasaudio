package com.xsasakihaise.hellasaudio.item;

import com.xsasakihaise.hellasaudio.ModItems;
import com.xsasakihaise.hellasaudio.server.MusicDiscManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Item that represents a custom HellasAudio disc. The disc identifier is stored in NBT so the
 * playback system knows which server-side recording to use.
 */
public class CustomDiscItem extends Item {
    private static final String DISC_ID_TAG = "DiscId";
    private static final String DISC_NAME_TAG = "DiscName";

    public CustomDiscItem(Properties properties) {
        super(properties);
    }

    /**
     * Attempts to trigger playback on right click when the server has the requested recording stored.
     */
    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            String discId = getDiscId(stack);
            if (discId == null || discId.isEmpty()) {
                serverPlayer.sendMessage(new StringTextComponent(TextFormatting.RED + "This disc is missing its recording."), Util.NIL_UUID);
                return ActionResult.fail(stack);
            }

            MusicDiscManager.getInstance().playDisc(serverPlayer.createCommandSourceStack().withSuppressedOutput(), discId, Collections.singleton(serverPlayer));
        }
        return ActionResult.sidedSuccess(stack, world.isClientSide);
    }

    /**
     * Shows the configured song name (if any) in gold so discs stand out from vanilla items.
     */
    @Override
    public ITextComponent getName(ItemStack stack) {
        String displayName = getDisplayName(stack);
        if (displayName != null && !displayName.isEmpty()) {
            return new StringTextComponent(displayName).withStyle(TextFormatting.GOLD);
        }
        String discId = getDiscId(stack);
        if (discId != null && !discId.isEmpty()) {
            return new StringTextComponent("Custom Disc: " + discId).withStyle(TextFormatting.GOLD);
        }
        return super.getName(stack);
    }

    /**
     * Adds extra tooltip information so players can differentiate discs even if their inventory icons are identical.
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        String displayName = getDisplayName(stack);
        if (displayName != null && !displayName.isEmpty()) {
            tooltip.add(new StringTextComponent(TextFormatting.GRAY + "Song: " + displayName));
        }
        String discId = getDiscId(stack);
        if (discId != null && !discId.isEmpty()) {
            tooltip.add(new StringTextComponent(TextFormatting.DARK_GRAY + "Disc ID: " + discId));
        }
        super.appendHoverText(stack, world, tooltip, flag);
    }

    /**
     * Builds a single-use custom disc item with pre-populated metadata tags. This is primarily used by server commands
     * when staff grant discs to players.
     */
    public static ItemStack createForDisc(String discId, String displayName) {
        ItemStack stack = new ItemStack(ModItems.CUSTOM_DISC.get());
        stack.getOrCreateTag().putString(DISC_ID_TAG, discId);
        if (displayName != null && !displayName.isEmpty()) {
            stack.getOrCreateTag().putString(DISC_NAME_TAG, displayName);
            stack.setHoverName(new StringTextComponent(displayName).withStyle(TextFormatting.GOLD));
        } else {
            stack.setHoverName(new StringTextComponent("Custom Disc: " + discId).withStyle(TextFormatting.GOLD));
        }
        return stack;
    }

    /**
     * @return disc identifier embedded in the item's NBT or {@code null} if missing.
     */
    @Nullable
    public static String getDiscId(ItemStack stack) {
        if (stack.hasTag() && stack.getTag() != null && stack.getTag().contains(DISC_ID_TAG)) {
            return stack.getTag().getString(DISC_ID_TAG);
        }
        return null;
    }

    /**
     * @return player-facing song name stored inside the item's NBT or {@code null} when unnamed.
     */
    @Nullable
    public static String getDisplayName(ItemStack stack) {
        if (stack.hasTag() && stack.getTag() != null && stack.getTag().contains(DISC_NAME_TAG)) {
            return stack.getTag().getString(DISC_NAME_TAG);
        }
        return null;
    }
}
