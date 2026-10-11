package neth6.soulspoon.world;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import neth6.soulspoon.rule.Rule;
import neth6.soulspoon.rule.RuleData;
import neth6.soulspoon.util.Cmd;
import neth6.soulspoon.util.Spots;

import java.util.UUID;

// Parte 2 del plan: reglas de entorno (se activan con /dedsafio regla <id> on|off).
public class WorldRules {
    // Capas de gas (en Y). 11 y 12 son eventos, 85 es la regla permanente.
    public static final int GAS_ALTO_Y = 250;        // [11] y >= 250
    public static final int GAS_PROFUNDO_Y = -1;     // [12] y <= -1
    public static final int GAS_PERMANENTE_Y = 150;  // [85] y > 150
    private static final float GAS_DAMAGE = 4.0f;

    private static final int WARDEN_CHECK_TICKS = 100;
    private static final int WARDEN_CHANCE = 6;

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(WorldRules::onServerTick);

        // [8] Warden muerto -> shrieker que puede invocar.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof Warden && entity.level() instanceof ServerLevel level
                    && RuleData.get(level.getServer()).isOn(Rule.SHRIEKER)) {
                placeShrieker(level, entity.blockPosition());
            }
        });

        // [45] Extincion: los animales pasivos desaparecen al cargarse.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            boolean passive = entity instanceof Cow || entity instanceof Pig
                    || entity instanceof Sheep || entity instanceof Chicken
                    || entity instanceof Rabbit;
            if (passive && RuleData.get(level.getServer()).isOn(Rule.EXTINCION)) {
                level.getServer().execute(entity::discard);
            }
        });

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
                return true;
            }
            RuleData rules = RuleData.get(serverLevel.getServer());
            Vec3 center = Vec3.atCenterOf(pos);
            // [21] Romper un panal invoca TNT.
            if ((state.is(Blocks.BEE_NEST) || state.is(Blocks.BEEHIVE)) && rules.isOn(Rule.PANALES)) {
                Cmd.run(serverLevel, center, "summon minecraft:tnt "
                        + center.x + " " + center.y + " " + center.z);
            }
            // [36] Romper una telarana invoca una arana de cueva.
            if (state.is(Blocks.COBWEB) && rules.isOn(Rule.ARANAS)) {
                EntityType.CAVE_SPIDER.spawn(serverLevel, pos, MobSpawnType.EVENT);
            }
            return true;
        });
    }

    // ---------- Tick ----------

    private static void onServerTick(MinecraftServer server) {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        RuleData rules = RuleData.get(server);
        int tick = server.getTickCount();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                continue;
            }
            ServerLevel level = player.serverLevel();

            // [39] Nether cerrado: matar a quien este dentro.
            if (tick % 20 == 0 && rules.isOn(Rule.NETHER_CERRADO)
                    && level.dimension() == Level.NETHER) {
                kill(player);
                continue;
            }
            // [22] Magma letal.
            if (tick % 5 == 0 && rules.isOn(Rule.MAGMA) && player.onGround()
                    && level.getBlockState(player.blockPosition().below()).is(Blocks.MAGMA_BLOCK)) {
                kill(player);
                continue;
            }
            if (tick % 20 == 0) {
                // [11/12/85] Gas toxico.
                if (inGas(player, level, rules)) {
                    player.hurt(level.damageSources().wither(), GAS_DAMAGE);
                }
                // [78] Nether letal: sin armadura completa, fuego infinito.
                if (rules.isOn(Rule.NETHER_LETAL) && level.dimension() == Level.NETHER
                        && !fullArmor(player)) {
                    player.setRemainingFireTicks(100);
                }
                // [49] Insomnia.
                if (rules.isOn(Rule.INSOMNIA)) {
                    insomnia(server, player);
                }
                // [90] Zombificacion.
                if (rules.isOn(Rule.ZOMBIFICACION)) {
                    infection(server, player);
                }
            }
            // [2] Warden natural en el Deep Dark.
            if (tick % WARDEN_CHECK_TICKS == 0 && rules.isOn(Rule.WARDEN)) {
                naturalWarden(player, level);
            }
        }
    }

    // ---------- Reglas ----------

    private static void kill(ServerPlayer player) {
        player.hurt(player.level().damageSources().genericKill(), Float.MAX_VALUE);
    }

    private static void placeShrieker(ServerLevel level, BlockPos pos) {
        BlockPos target = level.getBlockState(pos).canBeReplaced() ? pos : pos.above();
        if (!level.getBlockState(target).canBeReplaced()) {
            return;
        }
        BlockState shrieker = Blocks.SCULK_SHRIEKER.defaultBlockState()
                .setValue(SculkShriekerBlock.CAN_SUMMON, true);
        level.setBlock(target, shrieker, 3);
    }

    private static boolean inGas(ServerPlayer player, ServerLevel level, RuleData rules) {
        if (level.dimension() != Level.OVERWORLD) {
            return false;
        }
        double y = player.getY();
        if (rules.isOn(Rule.GAS) && y > GAS_PERMANENTE_Y) {
            return true;
        }
        if (EventManager.isActive(EventManager.WorldEvent.GAS_ALTO) && y >= GAS_ALTO_Y) {
            return true;
        }
        return EventManager.isActive(EventManager.WorldEvent.GAS_PROFUNDO) && y <= GAS_PROFUNDO_Y;
    }

    private static boolean fullArmor(ServerPlayer player) {
        return !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                && !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                && !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                && !player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
    }

    private static void naturalWarden(ServerPlayer player, ServerLevel level) {
        if (!level.getBiome(player.blockPosition()).is(Biomes.DEEP_DARK)
                || level.random.nextInt(WARDEN_CHANCE) != 0) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(64);
        if (!level.getEntities(EntityType.WARDEN, area, w -> true).isEmpty()) {
            return;
        }
        BlockPos spot = Spots.ground(level, player.blockPosition(), 24, 30);
        if (spot != null && spot.distSqr(player.blockPosition()) > 12 * 12) {
            EntityType.WARDEN.spawn(level, spot, MobSpawnType.EVENT);
        }
    }

    // ---------- Insomnia [49] ----------

    public static boolean isInsomniaHigh(ServerPlayer player) {
        return PlayerState.get(player.server).insomnia(player.getUUID()) >= PlayerState.INSOMNIA_HIGH;
    }

    private static void insomnia(MinecraftServer server, ServerPlayer player) {
        PlayerState state = PlayerState.get(server);
        UUID id = player.getUUID();
        if (player.isSleepingLongEnough()) {
            state.setInsomnia(id, 0);
            return;
        }
        int value = state.insomnia(id) + 1;
        state.setInsomnia(id, value);
        if (value % 10 == 0) {
            int percent = Math.min(100, value * 100 / PlayerState.INSOMNIA_HIGH);
            player.displayClientMessage(Component.literal(
                    "Insomnia: " + percent + "%" + (percent >= 100 ? " (ALTA)" : "")), true);
        }
    }

    // ---------- Zombificacion permanente [90] ----------

    private static void infection(MinecraftServer server, ServerPlayer player) {
        PlayerState state = PlayerState.get(server);
        UUID id = player.getUUID();
        int immune = state.immune(id);
        if (immune > 0) {
            state.setImmune(id, immune - 1);
            return;
        }
        int left = state.infection(id) - 1;
        if (left > 0) {
            state.setInfection(id, left);
            if (left <= 60) {
                player.displayClientMessage(Component.literal(
                        "Infeccion: " + left + " s"), true);
            }
            return;
        }
        // Contador en 0: pierdes un totem o mueres.
        state.setInfection(id, PlayerState.INFECTION_START);
        if (takeTotem(player)) {
            player.displayClientMessage(Component.literal(
                    "La infeccion consumio uno de tus totems"), false);
        } else {
            kill(player);
        }
    }

    private static boolean takeTotem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    // La Pildora de Zombificacion: 4 horas de inmunidad y contador reiniciado.
    public static void givePill(MinecraftServer server, ServerPlayer player) {
        PlayerState state = PlayerState.get(server);
        state.setImmune(player.getUUID(), PlayerState.PILL_IMMUNITY);
        state.setInfection(player.getUUID(), PlayerState.INFECTION_START);
    }

    // Se mantiene aqui para que EventManager y otros la reutilicen.
    public static BlockPos findGround(ServerLevel level, BlockPos around, int range, int tries) {
        return Spots.ground(level, around, range, tries);
    }
}
