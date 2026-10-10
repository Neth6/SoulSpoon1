package neth6.soulspoon.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import neth6.soulspoon.rule.Rule;
import neth6.soulspoon.rule.RuleData;

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
                        )
        );
    }

    private static CompletableFuture<Suggestions> suggestRules(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                Arrays.stream(Rule.values()).map(Rule::id), builder);
    }

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
}