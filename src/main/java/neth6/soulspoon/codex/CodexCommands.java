package neth6.soulspoon.codex;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CodexCommands {
    private CodexCommands() {}

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("codex")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            player.displayClientMessage(Component.literal(
                                    "Codex: pulsa K para abrir la interfaz."), false);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("editor")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Editor autorizado. Abre el Codex con K y pulsa Editor."),
                                            false
                                    );
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("estado")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    CodexSavedData data = CodexSavedData.get(context.getSource().getServer());
                                    player.displayClientMessage(Component.literal(
                                            "Misión de prueba completada: " +
                                                    data.isMissionCompleted(player.getUUID(), "first_wood")
                                    ), false);
                                    return Command.SINGLE_SUCCESS;
                                }))
                )
        );
    }
}
