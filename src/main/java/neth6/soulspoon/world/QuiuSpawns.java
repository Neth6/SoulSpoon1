package neth6.soulspoon.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

import neth6.soulspoon.mob.Variant;
import neth6.soulspoon.mob.VariantManager;
import neth6.soulspoon.mob.Variants;
import neth6.soulspoon.util.Spots;

import java.util.List;
import java.util.Map;

// Fauna del Centro de Quiu: que mobs aparecen en cada bioma de la dimension.
public final class QuiuSpawns {
    private QuiuSpawns() {
    }

    private static final Map<String, List<String>> BY_BIOME = Map.of(
            "sabana_quiu", List.of("dromoraptor_rojo", "dromoraptor_azul", "dromoraptor_verde", "plumosaurio"),
            "acuiferos_quiu", List.of("narval"),
            "cuevas_quiu", List.of("reptisaurio_salvaje", "reptisaurio_emboscador", "kragtops",
                    "cerebro_amarillo", "cerebro_rojo"),
            "asentamientos_reptisaurios", List.of("reptisaurio_guerrero", "reptisaurio_arquero",
                    "reptisaurio_lanza"),
            "cielos_quiu", List.of("dactylo_bebe", "dactylo_adulto")
    );

    private static final int MAX_NEARBY = 6;

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(QuiuSpawns::onTick);
    }

    private static void onTick(MinecraftServer server) {
        if (server.getTickCount() % 100 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                continue;
            }
            ServerLevel level = player.serverLevel();
            if (level.dimension() != Dimensions.QUIU) {
                continue;
            }
            String biome = level.getBiome(player.blockPosition()).unwrapKey()
                    .map(key -> key.location().getPath()).orElse("");
            List<String> table = BY_BIOME.get(biome);
            if (table == null || level.random.nextInt(2) != 0) {
                continue;
            }
            Variant variant = Variants.byId(table.get(level.random.nextInt(table.size())));
            if (variant == null) {
                continue;
            }
            int nearby = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64),
                    mob -> mob.getTags().contains(variant.tag())).size();
            if (nearby >= MAX_NEARBY) {
                continue;
            }
            BlockPos pos = switch (variant.getPlace()) {
                case GROUND -> Spots.ground(level, player.blockPosition(), 24, 15);
                case AIR -> Spots.air(level, player.blockPosition(), 20, 8);
                case WATER -> Spots.water(level, player.blockPosition(), 16);
            };
            if (pos != null && pos.distSqr(player.blockPosition()) > 8 * 8) {
                VariantManager.spawn(level, pos, variant, false);
            }
        }
    }
}
