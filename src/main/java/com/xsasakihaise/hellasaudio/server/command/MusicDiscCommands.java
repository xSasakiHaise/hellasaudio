package com.xsasakihaise.hellasaudio.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xsasakihaise.hellasaudio.server.MusicDiscManager;
import com.xsasakihaise.hellasaudio.server.permission.PermissionHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.ItemHandlerHelper;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

/**
 * Brigadier command wiring for all HellasAudio related actions.
 */
public final class MusicDiscCommands {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final SuggestionProvider<CommandSource> DISC_SUGGESTIONS = (context, builder) -> {
        MusicDiscManager.getInstance().getAllDiscs().forEach(metadata ->
                builder.suggest(metadata.getDiscId(), new StringTextComponent(metadata.getDisplayName())));
        return builder.buildFuture();
    };

    private MusicDiscCommands() {
    }

    /**
     * Entrypoint invoked by {@link com.xsasakihaise.hellasaudio.server.ServerEventHandler} to register the entire
     * command tree under the root "/hellas" literal.
     */
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("hellas").then(createAudioLiteral()));
    }

    private static LiteralArgumentBuilder<CommandSource> createAudioLiteral() {
        LiteralArgumentBuilder<CommandSource> audio = Commands.literal("audio");
        attachAudioCommands(audio);
        return audio;
    }

    /**
     * Adds all subcommands (upload, play, list, name, remove, give) to the provided literal. Splitting this out keeps
     * the tree definition readable.
     */
    private static void attachAudioCommands(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(Commands.literal("upload")
                .executes(context -> {
                    context.getSource().sendSuccess(new StringTextComponent(
                            "Upload from a client with /hellas audio upload <disc_id> <path_to_mp3>. Place MP3 files in the hellasaudio/uploads folder under your Minecraft directory."), false);
                    return 1;
                }))
                .then(Commands.literal("play")
                        .requires(source -> PermissionHandler.hasCommandPermission(source, PermissionHandler.PLAY_PERMISSION, 2))
                        .then(Commands.argument("discId", StringArgumentType.string())
                                .suggests(DISC_SUGGESTIONS)
                                .executes(context -> {
                                    String discId = StringArgumentType.getString(context, "discId");
                                    MusicDiscManager.getInstance().playDisc(context.getSource(), discId, null);
                                    return 1;
                                })
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> {
                                            String discId = StringArgumentType.getString(context, "discId");
                                            Collection<ServerPlayerEntity> targets = EntityArgument.getPlayers(context, "targets");
                                            MusicDiscManager.getInstance().playDisc(context.getSource(), discId, targets);
                                            return targets.size();
                                        }))))
                .then(Commands.literal("list")
                        .requires(source -> PermissionHandler.hasCommandPermission(source, PermissionHandler.LIST_PERMISSION, 2))
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            if (MusicDiscManager.getInstance().getAllDiscs().isEmpty()) {
                                source.sendSuccess(new StringTextComponent("No custom discs have been uploaded yet."), false);
                            } else {
                                source.sendSuccess(new StringTextComponent(TextFormatting.GOLD + "Custom HellasAudio discs:"), false);
                                MusicDiscManager.getInstance().getAllDiscs().forEach(metadata ->
                                        source.sendSuccess(new StringTextComponent("- " + metadata.getDisplayName() + " [" + metadata.getDiscId() + "] (" + metadata.getUploader() + ", " + FORMATTER.format(metadata.getUploadedAt()) + ")"), false));
                            }
                            return 1;
                        }))
                .then(Commands.literal("name")
                        .requires(source -> PermissionHandler.hasCommandPermission(source, PermissionHandler.NAME_PERMISSION, 2))
                        .then(Commands.argument("discId", StringArgumentType.string())
                                .suggests(DISC_SUGGESTIONS)
                                .then(Commands.argument("displayName", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String discId = StringArgumentType.getString(context, "discId");
                                            String displayName = StringArgumentType.getString(context, "displayName");
                                            MusicDiscManager manager = MusicDiscManager.getInstance();
                                            if (!manager.getMetadata(discId).isPresent()) {
                                                context.getSource().sendFailure(new StringTextComponent("Unknown disc id: " + discId));
                                                return 0;
                                            }
                                            if (!manager.renameDisc(discId, displayName)) {
                                                context.getSource().sendFailure(new StringTextComponent("Display name must not be empty."));
                                                return 0;
                                            }
                                            MusicDiscManager.DiscMetadata metadata = manager.getMetadata(discId).orElse(null);
                                            if (metadata != null) {
                                                context.getSource().sendSuccess(new StringTextComponent("Renamed disc '" + discId + "' to '" + metadata.getDisplayName() + "'."), true);
                                            }
                                            return 1;
                                        }))))
                .then(Commands.literal("remove")
                        .requires(source -> PermissionHandler.hasCommandPermission(source, PermissionHandler.REMOVE_PERMISSION, 2))
                        .then(Commands.argument("discId", StringArgumentType.string())
                                .suggests(DISC_SUGGESTIONS)
                                .executes(context -> {
                                    String discId = StringArgumentType.getString(context, "discId");
                                    boolean removed = MusicDiscManager.getInstance().removeDisc(discId);
                                    if (removed) {
                                        context.getSource().sendSuccess(new StringTextComponent("Removed disc '" + discId + "'."), true);
                                        return 1;
                                    }
                                    context.getSource().sendFailure(new StringTextComponent("Unknown disc id: " + discId));
                                    return 0;
                                })))
                .then(Commands.literal("give")
                        .requires(source -> PermissionHandler.hasCommandPermission(source, PermissionHandler.GIVE_PERMISSION, 2))
                        .then(Commands.argument("discId", StringArgumentType.string())
                                .suggests(DISC_SUGGESTIONS)
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            String discId = StringArgumentType.getString(context, "discId");
                                            ServerPlayerEntity target = EntityArgument.getPlayer(context, "target");
                                            MusicDiscManager manager = MusicDiscManager.getInstance();
                                            MusicDiscManager.DiscMetadata metadata = manager.getMetadata(discId).orElse(null);
                                            if (metadata == null) {
                                                context.getSource().sendFailure(new StringTextComponent("Unknown disc id: " + discId));
                                                return 0;
                                            }

                                            ItemStack stack = manager.createDiscItem(discId);
                                            if (stack.isEmpty()) {
                                                context.getSource().sendFailure(new StringTextComponent("Failed to build item for disc '" + discId + "'."));
                                                return 0;
                                            }

                                            ItemHandlerHelper.giveItemToPlayer(target, stack);
                                            target.sendMessage(new StringTextComponent(TextFormatting.GREEN + "You received custom disc '" + metadata.getDisplayName() + "'."), Util.NIL_UUID);
                                            context.getSource().sendSuccess(new StringTextComponent("Gave '" + metadata.getDisplayName() + "' (" + discId + ") to " + target.getScoreboardName() + "."), true);
                                            return 1;
                                        }))));
    }
}
