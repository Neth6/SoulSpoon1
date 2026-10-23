package neth6.soulspoon.world.limbo;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.mob.Variant;
import neth6.soulspoon.mob.VariantManager;
import neth6.soulspoon.mob.Variants;
import neth6.soulspoon.util.Spots;
import neth6.soulspoon.world.Dimensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

// Parte 5: el Limbo. Cementerio oscuro con huesos y lapidas, charcos de liquido espectral
// (1 corazon cada 4 s), cofres que se rellenan cada 3 horas, Corazon del Limbo y fantasmas.
public final class Limbo {
    private Limbo() {
    }

    // Ultimo bloque solido del suelo del Limbo (bedrock en y=0, blackstone en y=1..3).
    private static final int FLOOR = 3;
    private static final int REFILL_TICKS = 3 * 60 * 60 * 20; // 3 horas
    private static final int LIQUID_TICKS = 80;               // 4 segundos
    private static final int RADIUS_CHUNKS = 3;

    private static final ResourceKey<LootTable> LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, SoulSpoon.id("chests/limbo"));

    private static final List<String> GHOSTS = List.of("fantasma_rojo", "fantasma_amarillo", "ojo_volador");

    public static Item CORAZON_LIMBO;

    private static final Map<UUID, Integer> LIQUID = new HashMap<>();

    public static void initialize() {
        CORAZON_LIMBO = Registry.register(BuiltInRegistries.ITEM, SoulSpoon.id("corazon_limbo"),
                new CorazonLimbo(new Item.Properties()
                        .stacksTo(16)
                        .component(DataComponents.ITEM_NAME, Component.literal("Corazón del Limbo"))));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register(entries -> entries.accept(CORAZON_LIMBO));

        ServerTickEvents.END_SERVER_TICK.register(Limbo::onTick);

        // /dedsafio limbo rellenar -> rellena ya todos los cofres del Limbo (para probar).
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("dedsafio")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("limbo")
                                .then(Commands.literal("rellenar").executes(ctx -> {
                                    int count = refillAll(ctx.getSource().getServer(), true);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Cofres del Limbo rellenados: " + count), true);
                                    return count;
                                })))));
    }

    // ---------- Tick ----------

    private static void onTick(MinecraftServer server) {
        ServerLevel limbo = server.getLevel(Dimensions.LIMBO);
        if (limbo == null) {
            return;
        }
        int tick = server.getTickCount();

        if (tick % 5 == 0) {
            liquidDamage(limbo);
        }
        if (tick % 10 == 0) {
            decorateAround(server, limbo);
        }
        if (tick % 100 == 0) {
            refillAll(server, false);
            spawns(limbo);
        }
    }

    // ---------- Liquido espectral ----------

    private static void liquidDamage(ServerLevel limbo) {
        for (ServerPlayer player : limbo.players()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                LIQUID.remove(player.getUUID());
                continue;
            }
            if (!player.isInWater()) {
                LIQUID.remove(player.getUUID());
                continue;
            }
            limbo.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 0.5, player.getZ(),
                    2, 0.3, 0.3, 0.3, 0.02);
            int time = LIQUID.merge(player.getUUID(), 5, Integer::sum);
            if (time >= LIQUID_TICKS) {
                LIQUID.put(player.getUUID(), 0);
                player.hurt(limbo.damageSources().magic(), 2.0f); // 1 corazon
                player.displayClientMessage(Component.literal("El líquido espectral te drena la vida"), true);
            }
        }
    }

    // ---------- Decoracion del cementerio ----------

    private static void decorateAround(MinecraftServer server, ServerLevel limbo) {
        LimboData data = LimboData.get(server);
        int budget = 4;
        for (ServerPlayer player : limbo.players()) {
            int pcx = player.blockPosition().getX() >> 4;
            int pcz = player.blockPosition().getZ() >> 4;
            for (int cx = pcx - RADIUS_CHUNKS; cx <= pcx + RADIUS_CHUNKS; cx++) {
                for (int cz = pcz - RADIUS_CHUNKS; cz <= pcz + RADIUS_CHUNKS; cz++) {
                    long key = ChunkPos.asLong(cx, cz);
                    if (data.isDecorated(key) || !limbo.hasChunk(cx, cz)) {
                        continue;
                    }
                    data.markDecorated(key);
                    decorate(limbo, data, cx, cz);
                    if (--budget <= 0) {
                        return;
                    }
                }
            }
        }
    }

    private static void decorate(ServerLevel level, LimboData data, int cx, int cz) {
        Random r = new Random(cx * 341873128712L + cz * 132897987541L + 7919L);
        int baseX = cx << 4;
        int baseZ = cz << 4;
        boolean[][] taken = new boolean[16][16];

        // La zona de llegada (cerca de 0,0) queda libre.
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double dx = baseX + x - 0.5;
                double dz = baseZ + z - 0.5;
                if (dx * dx + dz * dz < 64) {
                    taken[x][z] = true;
                }
            }
        }

        // Charcos de liquido espectral.
        if (r.nextInt(4) == 0) {
            int radius = 2 + r.nextInt(2);
            int px = 4 + r.nextInt(8);
            int pz = 4 + r.nextInt(8);
            if (free(taken, px, pz, radius + 1)) {
                for (int x = px - radius; x <= px + radius; x++) {
                    for (int z = pz - radius; z <= pz + radius; z++) {
                        if ((x - px) * (x - px) + (z - pz) * (z - pz) <= radius * radius) {
                            set(level, baseX + x, FLOOR, baseZ + z, Blocks.WATER.defaultBlockState());
                            set(level, baseX + x, FLOOR - 1, baseZ + z, Blocks.WATER.defaultBlockState());
                            taken[x][z] = true;
                        } else if ((x - px) * (x - px) + (z - pz) * (z - pz) <= (radius + 1) * (radius + 1)) {
                            set(level, baseX + x, FLOOR, baseZ + z, Blocks.SOUL_SOIL.defaultBlockState());
                            taken[x][z] = true;
                        }
                    }
                }
                set(level, baseX + px + radius + 1, FLOOR + 1, baseZ + pz, Blocks.SOUL_LANTERN.defaultBlockState());
            }
        }

        // Costillar gigante (esqueleto fosil).
        if (r.nextInt(6) == 0) {
            int x0 = 2 + r.nextInt(6);
            int z0 = 2 + r.nextInt(8);
            if (free(taken, x0 + 3, z0 + 2, 4)) {
                BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
                for (int i = 0; i <= 6; i++) {
                    set(level, baseX + x0 + i, FLOOR + 1, baseZ + z0 + 2, bone);
                    taken[x0 + i][z0 + 2] = true;
                }
                for (int i = 1; i <= 5; i += 2) {
                    for (int y = 1; y <= 3; y++) {
                        set(level, baseX + x0 + i, FLOOR + y, baseZ + z0, bone);
                        set(level, baseX + x0 + i, FLOOR + y, baseZ + z0 + 4, bone);
                    }
                    for (int z = 0; z <= 4; z++) {
                        set(level, baseX + x0 + i, FLOOR + 4, baseZ + z0 + z, bone);
                    }
                    for (int z = 0; z <= 4; z++) {
                        taken[x0 + i][z0 + z] = true;
                    }
                }
                set(level, baseX + x0 - 1, FLOOR + 1, baseZ + z0 + 2, skull(r));
            }
        }

        // Lapidas con tierra de almas.
        int graves = 4 + r.nextInt(5);
        for (int i = 0; i < graves; i++) {
            int x = 1 + r.nextInt(14);
            int z = 1 + r.nextInt(14);
            if (taken[x][z]) {
                continue;
            }
            taken[x][z] = true;
            set(level, baseX + x, FLOOR, baseZ + z, Blocks.SOUL_SOIL.defaultBlockState());
            int height = 1 + r.nextInt(2);
            Block wall = r.nextBoolean() ? Blocks.BLACKSTONE_WALL : Blocks.COBBLED_DEEPSLATE_WALL;
            for (int y = 1; y <= height; y++) {
                set(level, baseX + x, FLOOR + y, baseZ + z, wall.defaultBlockState());
            }
            BlockState top = r.nextInt(3) == 0 ? skull(r) : Blocks.BLACKSTONE_SLAB.defaultBlockState();
            set(level, baseX + x, FLOOR + height + 1, baseZ + z, top);
        }

        // Montones de huesos y telaranas.
        int piles = 3 + r.nextInt(4);
        for (int i = 0; i < piles; i++) {
            int x = 1 + r.nextInt(14);
            int z = 1 + r.nextInt(14);
            if (taken[x][z]) {
                continue;
            }
            taken[x][z] = true;
            set(level, baseX + x, FLOOR + 1, baseZ + z, Blocks.BONE_BLOCK.defaultBlockState());
            if (r.nextInt(2) == 0) {
                set(level, baseX + x, FLOOR + 2, baseZ + z, skull(r));
            } else if (r.nextInt(2) == 0) {
                set(level, baseX + x, FLOOR + 2, baseZ + z, Blocks.COBWEB.defaultBlockState());
            }
        }

        // Cofre con loot.
        if (r.nextInt(3) == 0) {
            for (int tries = 0; tries < 6; tries++) {
                int x = 1 + r.nextInt(14);
                int z = 1 + r.nextInt(14);
                if (taken[x][z]) {
                    continue;
                }
                taken[x][z] = true;
                BlockPos pos = new BlockPos(baseX + x, FLOOR + 1, baseZ + z);
                BlockState chest = Blocks.CHEST.defaultBlockState()
                        .setValue(ChestBlock.FACING, Direction.from2DDataValue(r.nextInt(4)));
                level.setBlock(pos, chest, 2);
                fill(level, pos);
                data.addChest(pos.asLong(), level.getGameTime() + REFILL_TICKS);
                break;
            }
        }
    }

    private static boolean free(boolean[][] taken, int cx, int cz, int radius) {
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                if (x < 0 || z < 0 || x > 15 || z > 15 || taken[x][z]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BlockState skull(Random r) {
        return Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r.nextInt(16));
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 2);
    }

    // ---------- Cofres ----------

    private static void fill(ServerLevel level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof ChestBlockEntity chest) {
            chest.clearContent();
            chest.setLootTable(LOOT, level.random.nextLong());
            chest.setChanged();
        }
    }

    // force = true rellena todos; si no, solo los que ya cumplieron las 3 horas.
    private static int refillAll(MinecraftServer server, boolean force) {
        ServerLevel limbo = server.getLevel(Dimensions.LIMBO);
        if (limbo == null) {
            return 0;
        }
        LimboData data = LimboData.get(server);
        long now = limbo.getGameTime();
        int count = 0;
        List<Long> gone = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : new ArrayList<>(data.chests().entrySet())) {
            BlockPos pos = BlockPos.of(entry.getKey());
            if (!limbo.isLoaded(pos)) {
                continue;
            }
            if (!limbo.getBlockState(pos).is(Blocks.CHEST)) {
                gone.add(entry.getKey());
                continue;
            }
            if (force || now >= entry.getValue()) {
                fill(limbo, pos);
                data.addChest(entry.getKey(), now + REFILL_TICKS);
                count++;
            }
        }
        for (long key : gone) {
            data.removeChest(key);
        }
        return count;
    }

    // ---------- Fantasmas y esqueletos ----------

    private static void spawns(ServerLevel limbo) {
        for (ServerPlayer player : limbo.players()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                continue;
            }
            if (limbo.random.nextInt(2) != 0) {
                continue;
            }
            if (limbo.random.nextInt(4) == 0) {
                spawnSkeleton(limbo, player);
                continue;
            }
            Variant variant = Variants.byId(GHOSTS.get(limbo.random.nextInt(GHOSTS.size())));
            if (variant == null) {
                continue;
            }
            int nearby = limbo.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64),
                    mob -> mob.getTags().contains(variant.tag())).size();
            if (nearby >= 4) {
                continue;
            }
            BlockPos pos = variant.getPlace() == Variant.Place.AIR
                    ? Spots.air(limbo, player.blockPosition(), 20, 3)
                    : Spots.ground(limbo, player.blockPosition(), 24, 15);
            if (pos != null && pos.distSqr(player.blockPosition()) > 8 * 8) {
                VariantManager.spawn(limbo, pos, variant, false);
            }
        }
    }

    private static void spawnSkeleton(ServerLevel limbo, ServerPlayer player) {
        int nearby = limbo.getEntitiesOfClass(Skeleton.class, player.getBoundingBox().inflate(64)).size();
        if (nearby >= 6) {
            return;
        }
        BlockPos pos = Spots.ground(limbo, player.blockPosition(), 24, 15);
        if (pos != null && pos.distSqr(player.blockPosition()) > 8 * 8) {
            EntityType.SKELETON.spawn(limbo, pos, MobSpawnType.EVENT);
        }
    }
}
