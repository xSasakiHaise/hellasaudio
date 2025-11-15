package com.xsasakihaise.hellasaudio.server.permission;

import com.xsasakihaise.hellasaudio.HellasAudio;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Helper around LuckPerms permission checks with a fallback to vanilla operator levels.
 */
public final class PermissionHandler {
    public static final String UPLOAD_PERMISSION = "hellasaudio.upload";
    public static final String PLAY_PERMISSION = "hellasaudio.command.play";
    public static final String LIST_PERMISSION = "hellasaudio.command.list";
    public static final String NAME_PERMISSION = "hellasaudio.command.name";
    public static final String REMOVE_PERMISSION = "hellasaudio.command.remove";
    public static final String GIVE_PERMISSION = "hellasaudio.command.give";

    private static final int OP_UPLOAD_LEVEL = 2;
    private static final String LUCKPERMS_PROVIDER_CLASS = "net.luckperms.api.LuckPermsProvider";

    private PermissionHandler() {
    }

    /**
     * Checks if the player can upload discs either by being a high-enough operator or by having the dedicated
     * LuckPerms node.
     */
    public static boolean canUpload(ServerPlayerEntity player) {
        return player.hasPermissions(OP_UPLOAD_LEVEL) || hasLuckPermsPermission(player, UPLOAD_PERMISSION);
    }

    /**
     * Utility invoked from Brigadier builders to grant access to subcommands. When LuckPerms is absent we fall back to
     * vanilla permission levels so standalone servers still function.
     */
    public static boolean hasCommandPermission(CommandSource source, String node, int fallbackLevel) {
        if (source.hasPermission(fallbackLevel)) {
            return true;
        }

        if (!(source.getEntity() instanceof ServerPlayerEntity)) {
            return false;
        }

        return hasLuckPermsPermission((ServerPlayerEntity) source.getEntity(), node);
    }

    /**
     * Reflectively checks LuckPerms (when present) so the mod does not have a hard dependency on it at compile time.
     */
    private static boolean hasLuckPermsPermission(ServerPlayerEntity player, String node) {
        if (!ModList.get().isLoaded("luckperms")) {
            return false;
        }

        try {
            Class<?> providerClass = Class.forName(LUCKPERMS_PROVIDER_CLASS);
            Method getMethod = providerClass.getMethod("get");
            Object luckPerms = getMethod.invoke(null);
            Method adapterMethod = luckPerms.getClass().getMethod("getPlayerAdapter", Class.class);
            Object adapter = adapterMethod.invoke(luckPerms, ServerPlayerEntity.class);
            Method hasPermissionMethod = adapter.getClass().getMethod("hasPermission", Object.class, String.class);
            Object result = hasPermissionMethod.invoke(adapter, player, node);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            HellasAudio.LOGGER.debug("LuckPerms unavailable for permission check", exception);
            return false;
        }
    }
}
