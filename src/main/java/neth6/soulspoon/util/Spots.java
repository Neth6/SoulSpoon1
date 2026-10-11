package neth6.soulspoon.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;

public final class Spots {
    private Spots() {
    }

    // Hueco de 2 de alto con suelo firme.
    public static BlockPos ground(ServerLevel level, BlockPos around, int range, int tries) {
        RandomSource random = level.random;
        for (int i = 0; i < tries; i++) {
            int x = around.getX() + random.nextInt(range * 2 + 1) - range;
            int z = around.getZ() + random.nextInt(range * 2 + 1) - range;
            for (int dy = 6; dy >= -6; dy--) {
                BlockPos pos = new BlockPos(x, around.getY() + dy, z);
                BlockPos floor = pos.below();
                if (level.getBlockState(pos).isAir()
                        && level.getBlockState(pos.above()).isAir()
                        && !level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) {
                    return pos;
                }
            }
        }
        return null;
    }

    // Aire libre un poco por encima del jugador (para voladores).
    public static BlockPos air(ServerLevel level, BlockPos around, int range, int up) {
        RandomSource random = level.random;
        for (int i = 0; i < 10; i++) {
            BlockPos pos = new BlockPos(
                    around.getX() + random.nextInt(range * 2 + 1) - range,
                    around.getY() + up + random.nextInt(4),
                    around.getZ() + random.nextInt(range * 2 + 1) - range);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return null;
    }

    // Dos bloques de agua seguidos.
    public static BlockPos water(ServerLevel level, BlockPos around, int range) {
        RandomSource random = level.random;
        for (int i = 0; i < 12; i++) {
            BlockPos pos = new BlockPos(
                    around.getX() + random.nextInt(range * 2 + 1) - range,
                    around.getY() + random.nextInt(7) - 3,
                    around.getZ() + random.nextInt(range * 2 + 1) - range);
            if (level.getFluidState(pos).is(FluidTags.WATER)
                    && level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                return pos;
            }
        }
        return null;
    }
}
