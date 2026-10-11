package neth6.soulspoon.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

import neth6.soulspoon.mob.Variant;
import neth6.soulspoon.mob.VariantData;
import neth6.soulspoon.mob.VariantManager;
import neth6.soulspoon.mob.Variants;
import neth6.soulspoon.rule.Rule;
import neth6.soulspoon.rule.RuleData;
import neth6.soulspoon.world.EventManager;
import neth6.soulspoon.world.WorldRules;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class RuleCommands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("dedsafio")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(RuleCommands::list)
                                        .then(Commands.literal("reglas")
                                                .executes(RuleCommands::list))
                                        .then(Commands.literal("regla")
                                                .then(Commands.argument(
                                                                "id", StringArgumentType.word())
                                                        .suggests(RuleCommands::suggestRules)
                                                        .then(Commands.literal("on")
                                                                .executes(ctx -> setRule(ctx, true)))
                                                        .then(Commands.literal("off")
                                                                .executes(ctx -> setRule(ctx, false)))))
                                        .then(Commands.literal("todas")
                                                .then(Commands.literal("on")
                                                        .executes(ctx -> setAll(ctx, true)))
                                                .then(Commands.literal("off")
                                                        .executes(ctx -> setAll(ctx, false))))
                                        .then(Commands.literal("evento")
                                                .then(Commands.literal("parar")
                                                        .executes(RuleCommands::stopEvents))
                                                .then(Commands.argument(
                                                                "nombre", StringArgumentType.word())
                                                        .suggests(RuleCommands::suggestEvents)
                                                        .executes(RuleCommands::startEvent)))
                                        .then(Commands.literal("pildora")
                                                .then(Commands.argument(
                                                                "jugador", EntityArgument.player())
                                                        .executes(RuleCommands::pill)))
                                        .then(Commands.literal("corazones")
                                                .then(Commands.argument(
                                                                "jugador", EntityArgument.player())
                                                        .executes(RuleCommands::restoreHearts)))
                                        .then(Commands.literal("mob")
                                                .then(Commands.literal("lista")
                                                        .executes(RuleCommands::mobList))
                                                .then(Commands.literal("invocar")
                                                        .then(Commands.argument(
                                                                        "id", StringArgumentType.word())
                                                                .suggests(RuleCommands::suggestMobs)
                                                                .executes(ctx -> summon(ctx, 1))
                                                                .then(Commands.argument("cantidad",
                                                                                IntegerArgumentType.integer(1, 20))
                                                                        .executes(ctx -> summon(ctx,
                                                                                IntegerArgumentType.getInteger(
                                                                                        ctx, "cantidad"))))))
                                                .then(Commands.literal("spawn")
                                                        .then(Commands.argument(
                                                                        "id", StringArgumentType.word())
                                                                .suggests(RuleCommands::suggestMobs)
                                                                .then(Commands.literal("on")
                                                                        .executes(ctx -> setSpawn(ctx, true)))
                                                                .then(Commands.literal("off")
                                                                        .executes(ctx -> setSpawn(ctx, false))))))
                        )
        );
    }

    // ---------- Sugerencias ----------

    private static CompletableFuture<Suggestions> suggestRules(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                Arrays.stream(Rule.values()).map(Rule::id), builder);
    }

    private static CompletableFuture<Suggestions> suggestEvents(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                Arrays.stream(EventManager.WorldEvent.values())
                        .map(EventManager.WorldEvent::id), builder);
    }

    private static CompletableFuture<Suggestions> suggestMobs(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                Variants.ALL.stream().map(Variant::id), builder);
    }

    // ---------- Reglas ----------

    private static int list(CommandContext<CommandSourceStack> context) {
        RuleData data = RuleData.get(context.getSource().getServer());
        StringBuilder text = new StringBuilder("Reglas DEDSAFIO:");
        for (Rule rule : Rule.values()) {
            text.append("\n")
                    .append(data.isOn(rule) ? "[ON]  " : "[OFF] ")
                    .append(rule.id())
                    .append(" - ")
                    .append(rule.description());
        }
        String result = text.toString();
        context.getSource().sendSuccess(() -> Component.literal(result), false);
        return 1;
    }

    private static int setRule(CommandContext<CommandSourceStack> context, boolean on) {
        String id = StringArgumentType.getString(context, "id");
        Rule rule = Rule.byId(id);
        if (rule == null) {
            context.getSource().sendFailure(Component.literal("No existe la regla " + id));
            return 0;
        }
        RuleData.get(context.getSource().getServer()).set(rule, on);
        context.getSource().sendSuccess(() -> Component.literal(
                "Regla " + rule.id() + (on ? " ACTIVADA" : " DESACTIVADA")), true);
        return 1;
    }

    private static int setAll(CommandContext<CommandSourceStack> context, boolean on) {
        RuleData data = RuleData.get(context.getSource().getServer());
        for (Rule rule : Rule.values()) {
            data.set(rule, on);
        }
        context.getSource().sendSuccess(() -> Component.literal(
                on ? "Todas las reglas ACTIVADAS" : "Todas las reglas DESACTIVADAS"), true);
        return 1;
    }

    // ---------- Eventos ----------

    private static int startEvent(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "nombre");
        EventManager.WorldEvent event = EventManager.WorldEvent.byId(name);
        if (event == null) {
            StringBuilder list = new StringBuilder("No existe el evento " + name + ". Eventos:");
            for (EventManager.WorldEvent e : EventManager.WorldEvent.values()) {
                list.append("\n").append(e.id()).append(" - ").append(e.description());
            }
            context.getSource().sendFailure(Component.literal(list.toString()));
            return 0;
        }
        EventManager.start(context.getSource().getServer(), event);
        return 1;
    }

    private static int stopEvents(CommandContext<CommandSourceStack> context) {
        EventManager.stopAll(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("Eventos detenidos"), true);
        return 1;
    }

    // ---------- Jugadores ----------

    // Pildora de Zombificacion: 4 horas de inmunidad y contador reiniciado.
    private static int pill(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "jugador");
        WorldRules.givePill(context.getSource().getServer(), player);
        context.getSource().sendSuccess(() -> Component.literal(
                player.getName().getString() + " tiene 4 horas de inmunidad"), true);
        return 1;
    }

    // Quita los corazones perdidos por el Fantasma Negro.
    private static int restoreHearts(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "jugador");
        Variants.restoreHearts(player);
        context.getSource().sendSuccess(() -> Component.literal(
                "Corazones restaurados a " + player.getName().getString()), true);
        return 1;
    }

    // ---------- Mobs ----------

    private static int mobList(CommandContext<CommandSourceStack> context) {
        VariantData data = VariantData.get(context.getSource().getServer());
        StringBuilder text = new StringBuilder("Mobs variante (spawn natural entre corchetes):");
        for (Variant variant : Variants.ALL) {
            String natural = variant.id() + (variant.name() != null ? " (" + variant.name() + ")" : "");
            text.append("\n").append(data.isOn(variant.id()) ? "[ON]  " : "[OFF] ").append(natural);
        }
        String result = text.toString();
        context.getSource().sendSuccess(() -> Component.literal(result), false);
        return 1;
    }

    private static int summon(CommandContext<CommandSourceStack> context, int amount)
            throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "id");
        Variant variant = Variants.byId(id);
        if (variant == null) {
            context.getSource().sendFailure(Component.literal("No existe el mob " + id));
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        int created = 0;
        for (int i = 0; i < amount; i++) {
            Mob mob = VariantManager.spawn(level, player.blockPosition(), variant, true);
            if (mob != null) {
                created++;
            }
        }
        int total = created;
        context.getSource().sendSuccess(() -> Component.literal(
                "Invocados " + total + " x " + variant.id()), true);
        return created;
    }

    private static int setSpawn(CommandContext<CommandSourceStack> context, boolean on) {
        String id = StringArgumentType.getString(context, "id");
        Variant variant = Variants.byId(id);
        if (variant == null) {
            context.getSource().sendFailure(Component.literal("No existe el mob " + id));
            return 0;
        }
        VariantData.get(context.getSource().getServer()).set(variant.id(), on);
        context.getSource().sendSuccess(() -> Component.literal(
                "Spawn natural de " + variant.id() + (on ? " ACTIVADO" : " DESACTIVADO")), true);
        return 1;
    }
}
