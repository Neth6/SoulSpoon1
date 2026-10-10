package neth6.soulspoon.wheel;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class ModWheel {
    public static void initialize() {
        WheelRegistry.registerDefaults();

        ServerTickEvents.END_SERVER_TICK.register(server -> WheelMenu.tickSpins());

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("ruleta")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            WheelMenu.open(player);
                                            return Command.SINGLE_SUCCESS;
                                        })
                        )
        );
    }
}