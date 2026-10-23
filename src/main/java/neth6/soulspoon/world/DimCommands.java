package neth6.soulspoon.world;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import neth6.soulspoon.SoulSpoon;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

// /dedsafio ir <overworld|limbo|centro_quiu>: viaje de prueba entre dimensiones.
public final class DimCommands {
    private DimCommands() {}

    private static final List<String> NAMES = List.of("overworld", "limbo", "centro_quiu");

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
                dispatcher.register(Commands.literal("dedsafio")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("ir")
                                .then(Commands.argument("dimension", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(NAMES, b))
                                        .executes(DimCommands::go)))));
    }

    private static int go(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "dimension");

        ResourceKey<Level> key = name.equals("overworld")
                ? Level.OVERWORLD
                : ResourceKey.create(Registries.DIMENSION, SoulSpoon.id(name));
        ServerLevel target = context.getSource().getServer().getLevel(key);
        if (target == null) {
            context.getSource().sendFailure(Component.literal("No existe la dimension '" + name + "'."));
            return 0;
        }
        BlockPos top = target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(0, 0, 0));
        player.teleportTo(target, 0.5, top.getY() + 1, 0.5, player.getYRot(), player.getXRot());
        context.getSource().sendSuccess(() -> Component.literal("Viajaste a " + name), true);
        return 1;
    }
}
