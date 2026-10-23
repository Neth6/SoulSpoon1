package neth6.soulspoon.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import neth6.soulspoon.wheel.WheelColor;
import neth6.soulspoon.wheel.WheelRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /anuncio <color> <titulo> | <mensaje>
 * Publica un anuncio con el icono y color de esa ruleta (titulo, chat y sonido).
 * Ejemplo: /anuncio rojo Se viene la tormenta | Buscad refugio
 * Si no pones "|" todo el texto va como titulo.
 */
public final class AnnounceCommand {
    private AnnounceCommand() {}

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("anuncio")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("color", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    for (WheelColor c : WheelColor.values()) b.suggest(c.id());
                                    return b.buildFuture();
                                })
                                .then(Commands.argument("texto", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            WheelColor color = WheelColor.byId(
                                                    StringArgumentType.getString(ctx, "color").toLowerCase());
                                            if (color == null) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "Color invalido: rojo, morado, verde, amarillo, cian, azul, naranja"));
                                                return 0;
                                            }
                                            String raw = StringArgumentType.getString(ctx, "texto");
                                            String title = raw;
                                            String subtitle = "";
                                            int bar = raw.indexOf('|');
                                            if (bar >= 0) {
                                                title = raw.substring(0, bar).trim();
                                                subtitle = raw.substring(bar + 1).trim();
                                            }
                                            WheelRegistry.broadcast(ctx.getSource().getServer(), color, title, subtitle);
                                            return 1;
                                        })))));
    }
}
