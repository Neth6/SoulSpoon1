package neth6.soulspoon.wheel;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import neth6.soulspoon.rule.Rule;
import neth6.soulspoon.rule.RuleData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WheelRegistry {
    // Un cambio que puede salir en la ruleta.
    public record Entry(WheelColor color, String id, String name,
                        String description, Consumer<MinecraftServer> action) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    public static void register(WheelColor color, String id, String name,
                                String description, Consumer<MinecraftServer> action) {
        ENTRIES.add(new Entry(color, id, name, description, action));
    }

    public static List<Entry> byColor(WheelColor color) {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (entry.color() == color) {
                result.add(entry);
            }
        }
        return result;
    }

    // Anuncio para todos: titulo grande, subtitulo, mensaje en el chat y sonido.
    public static void broadcast(MinecraftServer server, WheelColor color,
                                 String title, String subtitle) {
        Component titleText = Component.literal(title).withStyle(color.format());
        Component subtitleText = Component.literal(subtitle);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(titleText));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleText));
            player.playNotifySound(SoundEvents.BEACON_ACTIVATE, SoundSource.MASTER, 1.0f, 1.0f);
        }
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("[" + color.colorName() + "] ")
                        .withStyle(color.format())
                        .append(Component.literal(title + ": " + subtitle)),
                false);
    }

    // Cambios que ya existen. Aqui se agregan los nuevos.
    public static void registerDefaults() {
        // NARANJA: una entrada por cada regla de la Parte 1 (se crean solas).
        for (Rule rule : Rule.values()) {
            register(WheelColor.NARANJA, "regla_" + rule.id(), rule.description(),
                    "Activa la regla '" + rule.id() + "'",
                    server -> {
                        RuleData.get(server).set(rule, true);
                        broadcast(server, WheelColor.NARANJA, "Nuevo cambio", rule.description());
                    });
        }

        // VERDE: ayuda de Eon.
        register(WheelColor.VERDE, "grieta_quiu", "Revelar la grieta del Centro de Quiu",
                "Muestra las coordenadas de la entrada",
                server -> broadcast(server, WheelColor.VERDE, "Eon descubrio la grieta",
                        "X: 698 | Z: -145. Hace falta un Creeper Nuclear y armadura de diamante"));

        // AZUL: misiones principales.
        register(WheelColor.AZUL, "dia1", "Dia 1: Aldeano Phora",
                "Pica y recoge un Aldeano Phora de Recursos",
                server -> broadcast(server, WheelColor.AZUL, "Mision del Dia 1",
                        "Pica y recoge un Aldeano Phora de Recursos dentro de un Cubo Phora"));
        register(WheelColor.AZUL, "dia2", "Dia 2: Bolsa Primitiva",
                "Crea una Bolsa Primitiva",
                server -> broadcast(server, WheelColor.AZUL, "Mision del Dia 2",
                        "Crea una Bolsa Primitiva (receta con la tecla G)"));
    }
}