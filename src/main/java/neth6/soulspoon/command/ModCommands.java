package neth6.soulspoon.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import neth6.soulspoon.soul.DeadPlayers;
import neth6.soulspoon.soul.ReviveManager;
import neth6.soulspoon.soul.SoulManager;
import neth6.soulspoon.soul.SpoonRecipe;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
                                        .then(recipeCommand())
                        )
        );
    }

    // /almas receta [agregar|quitar|reiniciar]
    private static LiteralArgumentBuilder<CommandSourceStack> recipeCommand() {
        return Commands.literal("receta")
                .executes(ModCommands::showRecipe)
                .then(Commands.literal("agregar")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests(ModCommands::suggestItems)
                                .then(Commands.argument("cantidad",
                                                IntegerArgumentType.integer(1, 64))
                                        .executes(ModCommands::addIngredient))))
                .then(Commands.literal("quitar")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests(ModCommands::suggestIngredients)
                                .executes(ModCommands::removeIngredient)))
                .then(Commands.literal("reiniciar")
                        .requires(source -> source.hasPermission(2))
                        .executes(ModCommands::resetRecipe));
    }

    private static void say(CommandContext<CommandSourceStack> context, String text) {
        context.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    // Autocompletado: todos los objetos registrados (incluidos los de otros mods).
    private static CompletableFuture<Suggestions> suggestItems(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(
                BuiltInRegistries.ITEM.keySet(), builder);
    }

    // Autocompletado: solo los ingredientes que ya estan en la receta.
    private static CompletableFuture<Suggestions> suggestIngredients(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        SpoonRecipe recipe = SpoonRecipe.get(context.getSource().getServer());
        return SharedSuggestionProvider.suggestResource(recipe.getAll().keySet(), builder);
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

    private static int showRecipe(CommandContext<CommandSourceStack> context) {
        SpoonRecipe recipe = SpoonRecipe.get(context.getSource().getServer());
        say(context, "Receta de la Cuchara de Almas: " + recipe.describe());
        return Command.SINGLE_SUCCESS;
    }

    private static int addIngredient(CommandContext<CommandSourceStack> context) {
        ResourceLocation id = ResourceLocationArgument.getId(context, "item");
        int amount = IntegerArgumentType.getInteger(context, "cantidad");

        // Debe ser un objeto registrado (de Minecraft o de otro mod instalado).
        if (!BuiltInRegistries.ITEM.containsKey(id)
                || BuiltInRegistries.ITEM.get(id) == Items.AIR) {
            say(context, "No existe ningun objeto con el ID " + id);
            return 0;
        }

        SpoonRecipe recipe = SpoonRecipe.get(context.getSource().getServer());
        recipe.set(id, amount);
        say(context, "Receta actualizada: " + recipe.describe());
        return Command.SINGLE_SUCCESS;
    }

    private static int removeIngredient(CommandContext<CommandSourceStack> context) {
        ResourceLocation id = ResourceLocationArgument.getId(context, "item");
        SpoonRecipe recipe = SpoonRecipe.get(context.getSource().getServer());

        if (recipe.remove(id)) {
            say(context, "Receta actualizada: " + recipe.describe());
        } else {
            say(context, id + " no estaba en la receta");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int resetRecipe(CommandContext<CommandSourceStack> context) {
        SpoonRecipe recipe = SpoonRecipe.get(context.getSource().getServer());
        recipe.reset();
        say(context, "Receta reiniciada: " + recipe.describe());
        return Command.SINGLE_SUCCESS;
    }
}