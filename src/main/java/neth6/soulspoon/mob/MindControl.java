package neth6.soulspoon.mob;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Control mental del Cerebro Rojo y posesion del Fantasma Amarillo:
// el jugador afectado ataca al aliado mas cercano.
public final class MindControl {
    private MindControl() {}

    private static final Map<UUID, Long> UNTIL = new HashMap<>();

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(MindControl::tick);
    }

    public static void apply(ServerPlayer player, int ticks) {
        long now = player.server.overworld().getGameTime();
        boolean already = UNTIL.containsKey(player.getUUID());
        UNTIL.put(player.getUUID(), now + ticks);
        if (!already) {
            player.displayClientMessage(Component.literal("Tu mente ya no es tuya..."), true);
        }
    }

    private static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % 10 != 0 || UNTIL.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Long until = UNTIL.get(player.getUUID());
            if (until == null) {
                continue;
            }
            if (until < now || !player.isAlive()) {
                UNTIL.remove(player.getUUID());
                continue;
            }
            ServerPlayer target = null;
            double best = 16.0; // 4 bloques al cuadrado
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (other == player || !other.isAlive() || other.isCreative() || other.isSpectator()
                        || other.level() != player.level()) {
                    continue;
                }
                double distance = player.distanceToSqr(other);
                if (distance < best) {
                    best = distance;
                    target = other;
                }
            }
            if (target != null) {
                player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                player.attack(target);
                player.swing(InteractionHand.MAIN_HAND, true);
            }
        }
    }
}
