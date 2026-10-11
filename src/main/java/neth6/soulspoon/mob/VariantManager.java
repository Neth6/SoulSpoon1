package neth6.soulspoon.mob;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

import neth6.soulspoon.rule.Rule;
import neth6.soulspoon.rule.RuleData;
import neth6.soulspoon.util.Spots;

import java.util.ArrayList;
import java.util.List;

// Hace aparecer las variantes y ejecuta sus comportamientos (tick, golpe, muerte...).
public class VariantManager {

    // Evita multiplicar el daño dos veces seguidas (regla podskiffx4).
    private static boolean amplifying = false;

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(VariantManager::onTick);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (amplifying) {
                return true;
            }

            Entity attacker = source.getEntity();

            // La variante golpea a un jugador.
            Variant attackerVariant = of(attacker);

            if (attackerVariant != null && entity instanceof ServerPlayer player) {
                if (attackerVariant.getHit() != null) {
                    attackerVariant.getHit().accept((Mob) attacker, player);
                }

                // Pod y Skiff Phora: daño x4.
                boolean podSkiff = attackerVariant.id().equals("pod_phora")
                        || attackerVariant.id().equals("skiff_phora");

                if (podSkiff && RuleData.get(player.server).isOn(Rule.POD_SKIFF_X4)) {
                    amplifying = true;
                    try {
                        player.hurt(source, amount * 4.0f);
                    } finally {
                        amplifying = false;
                    }
                    return false;
                }
            }

            // Le hacen daño a la variante.
            Variant victimVariant = of(entity);

            if (victimVariant != null
                    && victimVariant.getHurt() != null
                    && attacker instanceof LivingEntity living) {
                victimVariant.getHurt().accept((Mob) entity, living);
            }

            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            Variant variant = of(entity);

            if (variant != null && variant.getDeath() != null) {
                variant.getDeath().accept((Mob) entity);
            }
        });

        // Desaparición: por ejemplo, un creeper que explota se descarta.
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            Variant variant = of(entity);

            if (variant != null
                    && variant.getEnd() != null
                    && entity.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
                variant.getEnd().accept((Mob) entity, level);
            }
        });
    }

    public static Variant of(Entity entity) {
        if (!(entity instanceof Mob) || entity.getTags().isEmpty()) {
            return null;
        }

        for (String tag : entity.getTags()) {
            Variant variant = Variants.byTag(tag);

            if (variant != null) {
                return variant;
            }
        }

        return null;
    }

    // Crea la variante en el mundo. Devuelve null si no se pudo.
    public static Mob spawn(
            ServerLevel level,
            BlockPos pos,
            Variant variant,
            boolean persistent
    ) {
        // CORREGIDO: spawn() devuelve Entity, no necesariamente Mob.
        Entity entity = variant.getType().spawn(level, pos, MobSpawnType.EVENT);

        // Comprueba que la entidad creada sea un Mob.
        if (!(entity instanceof Mob mob)) {
            return null;
        }

        if (variant.getName() != null) {
            mob.setCustomName(Component.literal(variant.getName()));
            mob.setCustomNameVisible(true);
        }

        mob.addTag(variant.tag());

        if (persistent) {
            mob.setPersistenceRequired();
        }

        if (variant.getSetup() != null) {
            variant.getSetup().accept(mob);
        }

        mob.setHealth(mob.getMaxHealth());

        return mob;
    }

    // ---------- Tick ----------

    private static void onTick(MinecraftServer server) {
        int tick = server.getTickCount();

        Roots.tick(server);

        if (tick % 5 == 0) {
            for (ServerLevel level : server.getAllLevels()) {
                List<Mob> tagged = new ArrayList<>();

                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof Mob mob && !mob.getTags().isEmpty()) {
                        tagged.add(mob);
                    }
                }

                for (Mob mob : tagged) {
                    Variant variant = of(mob);

                    if (variant != null && variant.getTick() != null && mob.isAlive()) {
                        variant.getTick().accept(mob, level);
                    }
                }
            }
        }

        if (tick % 200 == 0) {
            naturalSpawns(server);
        }
    }

    // Aparición natural: solo variantes activadas con
    // /dedsafio mob spawn <id> on.
    private static void naturalSpawns(MinecraftServer server) {
        VariantData data = VariantData.get(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                continue;
            }

            ServerLevel level = player.serverLevel();

            for (Variant variant : Variants.ALL) {
                if (variant.getSpawnPredicate() == null
                        || !data.isOn(variant.id())
                        || !variant.getSpawnPredicate().test(player, level)
                        || level.random.nextInt(variant.getChance()) != 0) {
                    continue;
                }

                int nearby = level.getEntitiesOfClass(
                        Mob.class,
                        player.getBoundingBox().inflate(64),
                        mob -> mob.getTags().contains(variant.tag())
                ).size();

                if (nearby >= 3) {
                    continue;
                }

                BlockPos pos = switch (variant.getPlace()) {
                    case GROUND -> Spots.ground(
                            level, player.blockPosition(), 24, 15
                    );
                    case AIR -> Spots.air(
                            level, player.blockPosition(), 20, 8
                    );
                    case WATER -> Spots.water(
                            level, player.blockPosition(), 16
                    );
                };

                if (pos != null && pos.distSqr(player.blockPosition()) > 64) {
                    spawn(level, pos, variant, false);
                }
            }
        }
    }
}
