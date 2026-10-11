package neth6.soulspoon.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.entity.NuclearCreeperEntity;

// Acceso a las dimensiones Centro de Quiu y Limbo, y a la grieta del Overworld.
public final class Dimensions {
    private Dimensions() {
    }

    public static final ResourceKey<Level> QUIU = key("centro_quiu");
    public static final ResourceKey<Level> LIMBO = key("limbo");

    // Coordenadas de la grieta (PDF): X 698, Z -145.
    public static final int RIFT_X = 698;
    public static final int RIFT_Z = -145;
    private static final int RIFT_RADIUS = 3;

    private static ResourceKey<Level> key(String path) {
        return ResourceKey.create(Registries.DIMENSION, SoulSpoon.id(path));
    }

    public static void initialize() {
        // Un Creeper Nuclear que explota cerca de la grieta la abre.
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (level.dimension() != Level.OVERWORLD
                    || entity.getRemovalReason() != Entity.RemovalReason.DISCARDED) {
                return;
            }
            boolean nuclear = entity instanceof NuclearCreeperEntity
                    || entity.getTags().contains("creeper_nuclear");
            if (!nuclear || (entity instanceof Creeper creeper && creeper.getSwellDir() <= 0)) {
                return;
            }
            double dx = entity.getX() - (RIFT_X + 0.5);
            double dz = entity.getZ() - (RIFT_Z + 0.5);
            if (dx * dx + dz * dz <= 12 * 12) {
                MinecraftServer server = level.getServer();
                server.execute(() -> openRift(server));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(Dimensions::onTick);
    }

    // ---------- Grieta ----------

    public static void openRift(MinecraftServer server) {
        RiftData data = RiftData.get(server);
        if (data.isOpen()) {
            return;
        }
        ServerLevel overworld = server.overworld();
        overworld.getChunk(RIFT_X >> 4, RIFT_Z >> 4);
        int top = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RIFT_X, RIFT_Z);
        int bottom = overworld.getMinBuildHeight() + 2;
        for (int dx = -RIFT_RADIUS; dx <= RIFT_RADIUS; dx++) {
            for (int dz = -RIFT_RADIUS; dz <= RIFT_RADIUS; dz++) {
                if (dx * dx + dz * dz > RIFT_RADIUS * RIFT_RADIUS) {
                    continue;
                }
                for (int y = top + 2; y >= bottom; y--) {
                    overworld.setBlock(new BlockPos(RIFT_X + dx, y, RIFT_Z + dz),
                            Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        data.setOpen(true);
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("La grieta del Centro de Quiu se ha abierto"), false);
    }

    public static void closeRift(MinecraftServer server) {
        RiftData.get(server).setOpen(false);
    }

    // ---------- Viajes ----------

    private static void onTick(MinecraftServer server) {
        if (server.getTickCount() % 5 != 0 || server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        boolean open = RiftData.get(server).isOpen();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }
            ServerLevel level = player.serverLevel();
            // Caer por la grieta lleva al Centro de Quiu.
            if (open && level.dimension() == Level.OVERWORLD) {
                double dx = player.getX() - (RIFT_X + 0.5);
                double dz = player.getZ() - (RIFT_Z + 0.5);
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RIFT_X, RIFT_Z);
                if (dx * dx + dz * dz <= 3.5 * 3.5 && player.getY() < top - 6) {
                    sendToQuiu(player);
                    continue;
                }
            }
            // En Quiu, el obsidiana llorosa de la llegada devuelve al Overworld.
            if (level.dimension() == QUIU && player.onGround()
                    && level.getBlockState(player.blockPosition().below()).is(Blocks.CRYING_OBSIDIAN)) {
                sendToOverworld(player, true);
            }
        }
    }

    private static int surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    public static void sendToQuiu(ServerPlayer player) {
        ServerLevel quiu = player.server.getLevel(QUIU);
        if (quiu == null) {
            player.displayClientMessage(Component.literal(
                    "La dimension centro_quiu no esta cargada (revisa data/soulspoon/dimension)"), false);
            return;
        }
        // Plataforma de salida: obsidiana llorosa en el centro rodeada de obsidiana.
        int y = surface(quiu, 0, 0);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = new BlockPos(dx, y - 1, dz);
                quiu.setBlock(pos, (dx == 0 && dz == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN)
                        .defaultBlockState(), 2);
            }
        }
        int landing = surface(quiu, 0, 3);
        player.teleportTo(quiu, 0.5, landing, 3.5, player.getYRot(), player.getXRot());
        player.displayClientMessage(Component.literal("Centro de Quiu"), true);
    }

    public static void sendToLimbo(ServerPlayer player) {
        ServerLevel limbo = player.server.getLevel(LIMBO);
        if (limbo == null) {
            player.displayClientMessage(Component.literal(
                    "La dimension limbo no esta cargada (revisa data/soulspoon/dimension)"), false);
            return;
        }
        limbo.getChunk(0, 0);
        player.teleportTo(limbo, 0.5, 4.0, 0.5, player.getYRot(), player.getXRot());
        player.displayClientMessage(Component.literal("Limbo"), true);
    }

    // fromRift = true: aparece al lado de la grieta; si no, en el spawn del mundo.
    public static void sendToOverworld(ServerPlayer player, boolean fromRift) {
        ServerLevel overworld = player.server.overworld();
        int x;
        int z;
        if (fromRift) {
            x = RIFT_X + 8;
            z = RIFT_Z;
        } else {
            BlockPos spawn = overworld.getSharedSpawnPos();
            x = spawn.getX();
            z = spawn.getZ();
        }
        int y = surface(overworld, x, z);
        player.teleportTo(overworld, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
    }
}
