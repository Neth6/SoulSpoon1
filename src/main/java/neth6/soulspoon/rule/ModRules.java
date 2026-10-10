package neth6.soulspoon.rule;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModRules {
    // Evita aplicar el multiplicador de dano dos veces seguidas.
    private static boolean applyingDamage = false;

    // Aire que tenia cada jugador en el tick anterior (para la regla del ahogo).
    private static final Map<UUID, Integer> LAST_AIR = new HashMap<>();

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (applyingDamage || !(entity instanceof ServerPlayer player)) {
                return true;
            }
            RuleData rules = RuleData.get(player.server);
            float multiplier = damageMultiplier(player, source, amount, rules);
            if (multiplier == 1.0f) {
                return true;
            }
            // Cancelamos el golpe original y lo repetimos con el dano multiplicado.
            applyingDamage = true;
            try {
                player.hurt(source, amount * multiplier);
            } finally {
                applyingDamage = false;
            }
            return false;
        });

        ServerTickEvents.END_SERVER_TICK.register(ModRules::onServerTick);

        // Regla "brotes": los items de brote que aparecen en el mundo desaparecen.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item
                    && item.getItem().is(ItemTags.SAPLINGS)
                    && RuleData.get(level.getServer()).isOn(Rule.BROTES)) {
                level.getServer().execute(item::discard);
            }
        });

        // Regla "mecanismos": usar botones o puertas mata.
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            if (!RuleData.get(serverPlayer.server).isOn(Rule.MECANISMOS)) {
                return InteractionResult.PASS;
            }
            BlockState state = level.getBlockState(hit.getBlockPos());
            if (state.is(BlockTags.BUTTONS) || state.is(BlockTags.DOORS)) {
                killPlayer(serverPlayer);
            }
            return InteractionResult.PASS;
        });
    }

    // ---------- Dano ----------

    private static float damageMultiplier(ServerPlayer player, DamageSource source,
                                          float amount, RuleData rules) {
        float multiplier = 1.0f;

        if (isPearlDamage(player, source, amount)) {
            if (rules.isOn(Rule.PEARL)) {
                multiplier = 2.0f; // 5 -> 10
            }
        } else if (source.is(DamageTypes.FALL) && rules.isOn(Rule.CAIDA_X2)) {
            multiplier = 2.0f;
        }

        if (source.is(DamageTypeTags.IS_FIRE)) {
            if (rules.isOn(Rule.FUEGO_X10)) {
                multiplier = 10.0f;
            } else if (rules.isOn(Rule.FUEGO_X3)) {
                multiplier = 3.0f;
            }
        }

        if (source.is(DamageTypes.DROWN) && rules.isOn(Rule.AHOGO)) {
            multiplier = 10.0f;
        }
        return multiplier;
    }

    // La ender pearl hace 5 de dano de tipo caida justo al teletransportarte.
    private static boolean isPearlDamage(ServerPlayer player, DamageSource source, float amount) {
        if (source.typeHolder().is(ResourceLocation.withDefaultNamespace("ender_pearl"))) {
            return true;
        }
        return source.is(DamageTypes.FALL)
                && amount == 5.0f
                && player.fallDistance < 3.0f
                && player.getCooldowns().isOnCooldown(Items.ENDER_PEARL);
    }

    // ---------- Tick ----------

    private static void onServerTick(MinecraftServer server) {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        RuleData rules = RuleData.get(server);
        int tick = server.getTickCount();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            fastAir(player, rules.isOn(Rule.AHOGO));

            if (rules.isOn(Rule.MECANISMOS)
                    && player.level().getBlockState(player.blockPosition())
                    .is(BlockTags.PRESSURE_PLATES)) {
                killPlayer(player);
            }
            if (tick % 2 == 0 && rules.isOn(Rule.ABSORCION)) {
                removeAbsorption(player);
            }
            if (tick % 10 == 0) {
                if (rules.isOn(Rule.LECHE)) {
                    clearMilk(player);
                }
                if (rules.isOn(Rule.PUERTAS)) {
                    doorsInWater(player);
                }
            }
            if (tick % 20 == 0 && rules.isOn(Rule.COMIDA)) {
                rot(player.getInventory());
            }
            if (tick % 100 == 0 && rules.isOn(Rule.COMIDA)) {
                rotNearbyContainers(player);
            }
        }
    }

    // Aire x5 mas rapido: por cada punto que baja, bajamos 4 mas.
    private static void fastAir(ServerPlayer player, boolean on) {
        UUID id = player.getUUID();
        if (!on) {
            LAST_AIR.remove(id);
            return;
        }
        int air = player.getAirSupply();
        Integer previous = LAST_AIR.get(id);
        if (previous != null && air < previous && air > -20) {
            // Nunca bajamos de -19: en -20 Minecraft aplica el dano de ahogo.
            air = Math.max(-19, air - (previous - air) * 4);
            player.setAirSupply(air);
        }
        LAST_AIR.put(id, air);
    }

    // Las manzanas doradas dan absorcion de 2 minutos; el totem solo 5 segundos.
    private static void removeAbsorption(ServerPlayer player) {
        MobEffectInstance effect = player.getEffect(MobEffects.ABSORPTION);
        if (effect != null && effect.getDuration() > 1000) {
            player.removeEffect(MobEffects.ABSORPTION);
            player.setAbsorptionAmount(0.0f);
        }
    }

    private static void clearMilk(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.MILK_BUCKET)) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    // Puertas y trampillas pegadas al agua se convierten en agua.
    private static void doorsInWater(ServerPlayer player) {
        Level level = player.level();
        BlockPos[] spots = {player.blockPosition(), BlockPos.containing(player.getEyePosition())};
        for (BlockPos pos : spots) {
            BlockState state = level.getBlockState(pos);
            boolean isDoor = state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS);
            if (isDoor && touchesWater(level, pos)) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            }
        }
    }

    private static boolean touchesWater(Level level, BlockPos pos) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    // ---------- Comida podrida ----------

    private static boolean isRottable(ItemStack stack) {
        return !stack.isEmpty()
                && !stack.is(Items.ROTTEN_FLESH)
                && (stack.is(ItemTags.MEAT) || stack.is(ItemTags.FISHES));
    }

    private static void rot(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (isRottable(stack)) {
                container.setItem(i, new ItemStack(Items.ROTTEN_FLESH, stack.getCount()));
            }
        }
    }

    // Cofres, barriles, hornos, etc. en los chunks cercanos al jugador.
    private static void rotNearbyContainers(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int chunkX = player.blockPosition().getX() >> 4;
        int chunkZ = player.blockPosition().getZ() >> 4;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof Container container) {
                        rot(container);
                    }
                }
            }
        }
    }

    // ---------- Mecanismos letales ----------

    private static void killPlayer(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        player.hurt(player.level().damageSources().genericKill(), Float.MAX_VALUE);
    }
}