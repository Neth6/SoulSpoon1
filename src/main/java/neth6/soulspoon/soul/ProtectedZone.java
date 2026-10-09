package neth6.soulspoon.soul;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class ProtectedZone {
    // Radio (en bloques) de la zona protegida alrededor del altar.
    public static final int RADIUS = 48;

    public static boolean isProtected(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return false;
        }

        AltarData altar = AltarData.get(serverLevel.getServer());
        if (!altar.hasAltar()) {
            return false;
        }

        BlockPos center = altar.getAltarPos();
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        return dx * dx + dz * dz <= RADIUS * RADIUS;
    }
}