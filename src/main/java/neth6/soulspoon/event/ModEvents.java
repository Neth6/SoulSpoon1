package neth6.soulspoon.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;

import neth6.soulspoon.soul.DeadPlayers;
import neth6.soulspoon.soul.ProtectedZone;
import neth6.soulspoon.soul.ReviveEffects;

public class ModEvents {
    // true = modo prueba: el jugador puede volver a entrar aunque este muerto.
    // Ponlo en false para probar el bloqueo real.
    public static final boolean TEST_MODE = true;

    public static void initialize() {
        // Cuando un jugador muere: se anota y se le expulsa.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                String name = player.getName().getString();
                DeadPlayers.get(player.server).markDead(player.getUUID(), name);
                player.connection.disconnect(Component.literal(
                        "Has muerto. Un companero debe revivirte."));
            }
        });

        // Cuando un jugador entra: si esta muerto, se le echa.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            boolean isDead = DeadPlayers.get(server).isDead(player.getUUID());
            if (isDead && !TEST_MODE) {
                handler.disconnect(Component.literal(
                        "Estas muerto. Un companero debe revivirte."));
            }
        });

        // Cuando un jugador reaparece: vuelve al altar con efectos.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // "alive" es true al volver del End: ese caso no es una resurreccion.
            if (alive) {
                return;
            }
            if (ReviveEffects.returnToAltar(newPlayer)) {
                newPlayer.displayClientMessage(Component.literal(
                        "Has vuelto a la vida en el altar"), true);
                newPlayer.server.getPlayerList().broadcastSystemMessage(
                        Component.literal(newPlayer.getName().getString()
                                + " ha regresado de entre los muertos"), false);
            }
        });

        // Zona protegida: solo el modo creativo puede romper bloques cerca del altar.
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (player.isCreative()) {
                return true;
            }
            if (ProtectedZone.isProtected(level, pos)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.literal(
                            "Esta zona esta protegida por el altar"), true);
                }
                return false;
            }
            return true;
        });

        // Zona protegida: los mobs hostiles se eliminan al aparecer.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Enemy
                    && ProtectedZone.isProtected(level, entity.blockPosition())) {
                level.getServer().execute(entity::discard);
            }
        });
    }
}