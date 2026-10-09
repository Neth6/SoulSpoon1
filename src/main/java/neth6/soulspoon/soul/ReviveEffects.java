package neth6.soulspoon.soul;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class ReviveEffects {
    // Efecto en el altar cuando alguien gasta un alma para revivir.
    public static void altarPulse(ServerPlayer reviver) {
        MinecraftServer server = reviver.server;
        AltarData altar = AltarData.get(server);
        if (!altar.hasAltar()) {
            return;
        }

        ServerLevel level = server.overworld();
        BlockPos pos = altar.getAltarPos();

        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.SOUL,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                40, 0.4, 0.6, 0.4, 0.05);
    }

    // El jugador reaparece sobre el altar con un efecto de resurreccion.
    // Devuelve false si no hay altar (entonces el respawn es el normal).
    public static boolean returnToAltar(ServerPlayer player) {
        MinecraftServer server = player.server;
        AltarData altar = AltarData.get(server);
        if (!altar.hasAltar()) {
            return false;
        }

        ServerLevel level = server.overworld();
        BlockPos pos = altar.getAltarPos();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;

        player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());

        level.playSound(null, pos, SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                x, y + 0.8, z, 60, 0.4, 0.8, 0.4, 0.3);
        return true;
    }
}