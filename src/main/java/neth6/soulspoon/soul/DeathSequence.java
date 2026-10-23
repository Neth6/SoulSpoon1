package neth6.soulspoon.soul;

import com.mojang.serialization.Codec;
import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.event.ModEvents;
import neth6.soulspoon.wheel.WheelAnim;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Muerte de un jugador, en este orden:
 *   1) la muerte NO ocurre: se cancela, queda en modo espectador y se anota como muerto
 *   2) todos ven la animacion de muerte (y suena el audio)
 *   3) al terminar la animacion se le expulsa del servidor/mundo
 * Cuando lo revivan y vuelva a entrar, reaparece en el altar en modo supervivencia.
 */
public final class DeathSequence {
    private DeathSequence() {}

    /** true mientras el jugador esta muerto-pendiente (se guarda con el jugador). */
    public static final AttachmentType<Boolean> PENDING = AttachmentRegistry.create(
            SoulSpoon.id("death_pending"),
            builder -> builder.initializer(() -> false).persistent(Codec.BOOL));

    private static final Set<UUID> RUNNING = new HashSet<>();

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return true;
            }
            if (RUNNING.contains(player.getUUID())) {
                return false;              // ya esta en la secuencia: no puede "morir" otra vez
            }
            if (player.isSpectator()) {
                return true;               // un espectador que cae al vacio, etc.: normal
            }
            start(player);
            return false;                  // la muerte normal se cancela
        });

        // Al entrar: si lo revivieron (o estamos en modo prueba), vuelve a la vida en el altar.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (!Boolean.TRUE.equals(player.getAttached(PENDING))) {
                return;
            }
            boolean stillDead = DeadPlayers.get(server).isDead(player.getUUID());
            if (stillDead && !ModEvents.TEST_MODE) {
                return;                    // ModEvents ya lo echa
            }
            server.execute(() -> restore(player));
        });
    }

    private static void start(ServerPlayer player) {
        MinecraftServer server = player.server;
        RUNNING.add(player.getUUID());
        player.setAttached(PENDING, true);
        DeadPlayers.get(server).markDead(player.getUUID(), player.getName().getString());

        // Mensaje de muerte para todos (como el de vanilla).
        server.getPlayerList().broadcastSystemMessage(player.getCombatTracker().getDeathMessage(), false);

        // Lo que llevaba se queda en el suelo (salvo keepInventory).
        if (!player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            player.getInventory().dropAll();
        }

        // Espectador, con la vida llena para que no vuelva a "morir" por un efecto pendiente.
        player.removeAllEffects();
        player.clearFire();
        player.setHealth(player.getMaxHealth());
        player.setGameMode(GameType.SPECTATOR);

        // Audio + animacion de muerte para todos.
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(SoulSpoon.id("muerte"));
        if (sound != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.playNotifySound(sound, SoundSource.MASTER, 1.0f, 1.0f);
            }
        }
        String anim = WheelAnim.has("muerte") ? "muerte" : "death";
        WheelAnim.play(server, anim, () -> {
            RUNNING.remove(player.getUUID());
            if (!player.hasDisconnected()) {
                player.connection.disconnect(Component.literal(
                        "Has muerto. Un companero debe revivirte."));
            }
        });
    }

    private static void restore(ServerPlayer player) {
        if (player.hasDisconnected()) {
            return;
        }
        player.setAttached(PENDING, false);
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        if (ReviveEffects.returnToAltar(player)) {
            player.displayClientMessage(Component.literal("Has vuelto a la vida en el altar"), true);
            player.server.getPlayerList().broadcastSystemMessage(
                    Component.literal(player.getName().getString() + " ha regresado de entre los muertos"), false);
        }
    }
}
