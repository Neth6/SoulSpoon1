package neth6.soulspoon.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import neth6.soulspoon.soul.DeadPlayers;

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
    }
}