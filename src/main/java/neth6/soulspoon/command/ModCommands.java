package neth6.soulspoon.command;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import neth6.soulspoon.soul.SoulManager;

public class ModCommands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        Commands.literal("almas")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int souls = SoulManager.getSouls(player);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Te quedan " + souls + " almas"), false);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.literal("gastar")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            boolean ok = SoulManager.spendSoul(player);
                                            int souls = SoulManager.getSouls(player);
                                            String text = ok
                                                    ? "Gastaste 1 alma. Te quedan " + souls
                                                    : "No te quedan almas";
                                            context.getSource().sendSuccess(() -> Component.literal(text), false);
                                            return Command.SINGLE_SUCCESS;
                                        }))
                ));
    }
}