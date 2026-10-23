package neth6.soulspoon.fx;

import neth6.soulspoon.SoulSpoon;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

// Manchas que cubren la pantalla (Creeper Pastel...). Es una imagen de la fuente
// soulspoon:splat mostrada como titulo, asi no hace falta codigo en el cliente.
public final class Splat {
    private Splat() {}

    public static void show(ServerPlayer player, String name, int stayTicks) {
        String glyph = switch (name) {
            case "pastel" -> "";
            default -> "";
        };
        Component text = Component.literal(glyph).setStyle(Style.EMPTY.withFont(SoulSpoon.id("splat")));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(2, stayTicks, 40));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
        player.connection.send(new ClientboundSetTitleTextPacket(text));
    }
}
