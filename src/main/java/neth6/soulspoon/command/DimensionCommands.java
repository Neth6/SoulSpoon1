package neth6.soulspoon.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import neth6.soulspoon.world.Dimensions;
import neth6.soulspoon.world.RiftData;

// /dedsafio dimension quiu|limbo|volver  y  /dedsafio dimension grieta abrir|cerrar|estado
public class DimensionCommands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("dedsafio")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("dimension")
                                .then(Commands.literal("quiu").executes(ctx -> {
                                    Dimensions.sendToQuiu(player(ctx));
                                    return 1;
                                }))
                                .then(Commands.literal("limbo").executes(ctx -> {
                                    Dimensions.sendToLimbo(player(ctx));
                                    return 1;
                                }))
                                .then(Commands.literal("volver").executes(ctx -> {
                                    Dimensions.sendToOverworld(player(ctx), false);
                                    return 1;
                                }))
                                .then(Commands.literal("grieta")
                                        .then(Commands.literal("abrir").executes(ctx -> {
                                            Dimensions.openRift(ctx.getSource().getServer());
                                            return 1;
                                        }))
                                        .then(Commands.literal("cerrar").executes(ctx -> {
                                            Dimensions.closeRift(ctx.getSource().getServer());
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "Grieta cerrada (el hueco no se rellena)"), true);
                                            return 1;
                                        }))
                                        .then(Commands.literal("estado").executes(ctx -> {
                                            boolean open = RiftData.get(ctx.getSource().getServer()).isOpen();
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "Grieta " + (open ? "ABIERTA" : "cerrada")
                                                            + " en X " + Dimensions.RIFT_X
                                                            + " Z " + Dimensions.RIFT_Z), false);
                                            return 1;
                                        }))))));
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        return ctx.getSource().getPlayerOrException();
    }
}
