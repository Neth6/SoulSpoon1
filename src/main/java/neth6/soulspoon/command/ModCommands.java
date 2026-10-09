package neth6.soulspoon.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import neth6.soulspoon.gui.RecipeMenu;
import neth6.soulspoon.soul.DeadPlayers;
import neth6.soulspoon.soul.ReviveManager;
import neth6.soulspoon.soul.SoulManager;

import java.util.Map;
import java.util.UUID;

public class ModCommands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("almas")
                                        .executes(ModCommands::showSouls)
                                        .then(Commands.literal("gastar")
                                                .executes(ModCommands::spendSoul))
                                        .then(Commands.literal("muertos")
                                                .executes(ModCommands::listDead))
                                        .then(Commands.literal("revivir")
                                                .then(Commands.argument(
                                                                "nombre", StringArgumentType.word())
                                                        .executes(ModCommands::revive)))
                                        .then(Commands.literal("perdonar")
                                                .requires(source -> source.hasPermission(2))
                                                .then(Commands.argument(
                                                                "nombre", StringArgumentType.word())
                                                        .executes(ModCommands::forgive)))
                                        .then(Commands.literal("recetas")
                                                .executes(ModCommands::openRecipes))
                        )
        );
    }

    private static void say(CommandContext<CommandSourceStack> context, String text) {
        context.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    private static int showSouls(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        say(context, "Te quedan " + SoulManager.getSouls(player) + " almas");
        return Command.SINGLE_SUCCESS;
    }

    private static int spendSoul(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean ok = SoulManager.spendSoul(player);
        int souls = SoulManager.getSouls(player);
        say(context, ok ? "Gastaste 1 alma. Te quedan " + souls : "No te quedan almas");
        return Command.SINGLE_SUCCESS;
    }

    private static int listDead(CommandContext<CommandSourceStack> context) {
        DeadPlayers dead = DeadPlayers.get(context.getSource().getServer());
        if (dead.getAll().isEmpty()) {
            say(context, "Nadie esta muerto");
        } else {
            say(context, "Muertos: " + String.join(", ", dead.getAll().values()));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int revive(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "nombre");
        ReviveManager.Result result = ReviveManager.revive(player, name);

        switch (result) {
            case SUCCESS -> say(context, name + " fue revivido. Te quedan "
                    + SoulManager.getSouls(player) + " almas");
            case NO_SOULS -> say(context, "No te quedan almas para revivir");
            case NOT_DEAD -> say(context, name + " no esta muerto");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int forgive(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "nombre");
        DeadPlayers dead = DeadPlayers.get(context.getSource().getServer());

        UUID found = null;
        for (Map.Entry<UUID, String> entry : dead.getAll().entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                found = entry.getKey();
                break;
            }
        }

        boolean ok = found != null && dead.revive(found);
        say(context, ok ? name + " fue perdonado" : "No hay ningun muerto con ese nombre");
        return Command.SINGLE_SUCCESS;
    }

    // Abre el menu de la receta: editable solo para operadores.
    private static int openRecipes(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean canEdit = context.getSource().hasPermission(2);
        RecipeMenu.open(player, canEdit);
        return Command.SINGLE_SUCCESS;
    }
}