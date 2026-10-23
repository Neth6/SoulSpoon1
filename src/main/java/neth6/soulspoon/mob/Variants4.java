package neth6.soulspoon.mob;


import neth6.soulspoon.entity.GeoMobs;
import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.entity.Gates;
import neth6.soulspoon.entity.ModEntities;
import neth6.soulspoon.fx.Splat;
import neth6.soulspoon.world.WorldRules;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

// Parte 4: variantes de los mobs con modelo propio. Se registran en Variants (para el huevo y los hooks).
// Los textos con tilde usan escapes Unicode para evitar problemas de codificacion en Windows.
public final class Variants4 {
    private Variants4() {}

    private static boolean done = false;

    public static void defineAll() {
        if (done) {
            return;
        }
        done = true;

        // [Creeper Pastel] al explotar mancha la pantalla de pastel a los jugadores cercanos.
        Variants.register(new Variant("creeper_pastel", "Creeper Pastel", GeoMobs.CREEPER_PASTEL)
                .end((mob, level) -> {
                    if (!(mob instanceof Creeper creeper) || creeper.getSwellDir() <= 0) {
                        return;
                    }
                    for (ServerPlayer player : level.players()) {
                        if (player.distanceToSqr(mob) <= 100.0) {
                            Splat.show(player, "pastel", 100);
                        }
                    }
                }));

        // [50] Dactylo Adulto: pesadillas del cielo con insomnia alta.
        Variants.register(new Variant("dactylo_adulto", "Dáctylo Adulto", ModEntities.DACTYLO_ADULTO)
                .tick((mob, level) -> hunt(mob, level, 48))
                .spawn(Variant.Place.AIR, (p, l) -> l.dimension() == Level.OVERWORLD && WorldRules.isInsomniaHigh(p))
                .chance(10));

        // [60] Cerebro Rojo: se sube a la cabeza, ciega, drena vida y controla la mente.
        Variants.register(new Variant("cerebro_rojo", "Cerebro Rojo", ModEntities.CEREBRO_ROJO)
                .tick((mob, level) -> brain(mob, level, true)));

        // Cerebro Amarillo: igual, pero sin control mental.
        Variants.register(new Variant("cerebro_amarillo", "Cerebro Amarillo", ModEntities.CEREBRO_AMARILLO)
                .tick((mob, level) -> brain(mob, level, false)));

        // [61] Medula Osea Cambiaformas (el cambio de skin e inventario queda para mas adelante).
        Variants.register(new Variant("medula_osea", "Médula Ósea Cambiaformas", ModEntities.MEDULA_OSEA)
                .tick((mob, level) -> hunt(mob, level, 40))
                .hit((mob, player) -> mob.heal(4.0f)));

        // [62][68] Reaper Pequeño: destierra con la hoz; sale en el Deep Dark.
        Variants.register(new Variant("reaper_pequeno", "Reaper Pequeño", ModEntities.REAPER_PEQUENO)
                .tick((mob, level) -> hunt(mob, level, 32))
                .hit((mob, player) -> banish(player))
                .spawn(Variant.Place.GROUND, (p, l) -> l.getBiome(p.blockPosition()).is(Biomes.DEEP_DARK))
                .chance(8));

        // [63] Fantasma Rojo: rapido, nativo del Limbo; suelta Esencia Roja.
        Variants.register(new Variant("fantasma_rojo", "Fantasma Rojo", ModEntities.FANTASMA_ROJO)
                .tick((mob, level) -> hunt(mob, level, 48))
                .death(mob -> {
                    if (mob.getRandom().nextInt(2) == 0) {
                        mob.spawnAtLocation(new ItemStack(ModEntities.ESENCIA_ROJA));
                    }
                })
                .spawn(Variant.Place.AIR, (p, l) -> inDimension(l, "limbo"))
                .chance(10));

        // [74] Fantasma Amarillo: fuerza el totem y posee al jugador.
        Variants.register(new Variant("fantasma_amarillo", "Fantasma Amarillo", ModEntities.FANTASMA_AMARILLO)
                .tick((mob, level) -> hunt(mob, level, 40))
                .hit((mob, player) -> {
                    forceTotem(player);
                    MindControl.apply(player, 100);
                })
                .spawn(Variant.Place.AIR, (p, l) -> inDimension(l, "limbo"))
                .chance(14));

        // [67] Kragtops: dano x3, suelta Manzanas de Ambar; vive en el Centro de Quiu.
        Variants.register(new Variant("kragtops", "Kragtops", ModEntities.KRAGTOPS)
                .death(mob -> mob.spawnAtLocation(
                        new ItemStack(ModEntities.MANZANA_AMBAR, 1 + mob.getRandom().nextInt(2))))
                .spawn(Variant.Place.GROUND, (p, l) -> inDimension(l, "centro_quiu"))
                .chance(12));

        // [83] Creeper Visceral: invulnerable salvo con el Altavoz; explota al acercarse.
        Variants.register(new Variant("creeper_visceral", "Creeper Visceral", ModEntities.CREEPER_VISCERAL)
                .tick((mob, level) -> {
                    hunt(mob, level, 32);
                    Player near = nearestPlayer(mob, level, 2.5);
                    if (near != null) {
                        level.explode(mob, mob.getX(), mob.getY(), mob.getZ(), 3.0f,
                                Level.ExplosionInteraction.MOB);
                        mob.discard();
                    }
                }));

        // [91] Ojo Volador: invulnerable salvo a la luz directa de la Linterna.
        Variants.register(new Variant("ojo_volador", "Ojo Volador", ModEntities.OJO_VOLADOR)
                .tick(Variants4::eyeTick)
                .hit((mob, player) -> player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0)))
                .spawn(Variant.Place.AIR, (p, l) -> inDimension(l, "limbo"))
                .chance(10));
    }

    // ---------- Utilidades ----------

    private static boolean inDimension(ServerLevel level, String path) {
        return level.dimension().location().equals(SoulSpoon.id(path));
    }

    private static Player nearestPlayer(Mob mob, ServerLevel level, double range) {
        return level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), range,
                EntitySelector.NO_CREATIVE_OR_SPECTATOR);
    }

    private static void hunt(Mob mob, ServerLevel level, double range) {
        if (mob.getTarget() == null) {
            Player player = nearestPlayer(mob, level, range);
            if (player != null) {
                mob.setTarget(player);
            }
        }
    }

    // Cerebros: saltan a la cabeza del jugador y lo drenan hasta que lo maten.
    private static void brain(Mob mob, ServerLevel level, boolean control) {
        if (!mob.isPassenger()) {
            Player near = nearestPlayer(mob, level, 20);
            if (near != null) {
                mob.setTarget(near);
                if (mob.distanceTo(near) < 1.6f) {
                    mob.startRiding(near, true);
                }
            }
            return;
        }
        if (mob.getVehicle() instanceof ServerPlayer victim && mob.tickCount % 20 < 5) {
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            victim.hurt(level.damageSources().magic(), 1.0f);
            mob.heal(1.0f);
            if (control) {
                MindControl.apply(victim, 100);
            }
        }
    }

    // Reaper: destierra al Limbo si existe la dimension; si no, lo deja ciego y lento.
    private static void banish(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel limbo = server.getLevel(ResourceKey.create(Registries.DIMENSION, SoulSpoon.id("limbo")));
        if (limbo != null) {
            var top = limbo.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new net.minecraft.core.BlockPos(0, 0, 0));
            player.teleportTo(limbo, 0.5, top.getY() + 1, 0.5, player.getYRot(), player.getXRot());
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
        }
    }

    // Fantasma Amarillo: gasta un totem del jugador y le aplica sus efectos.
    private static void forceTotem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                stack.shrink(1);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                player.level().broadcastEntityEvent(player, (byte) 35);
                return;
            }
        }
    }

    // Ojo Volador: la luz directa de la Linterna es lo unico que le hace dano.
    private static void eyeTick(Mob mob, ServerLevel level) {
        hunt(mob, level, 40);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(mob) > 24.0 * 24.0
                    || !Gates.holds(player, Gates.LINTERNA)) {
                continue;
            }
            Vec3 look = player.getLookAngle();
            Vec3 toEye = mob.getEyePosition().subtract(player.getEyePosition()).normalize();
            if (look.dot(toEye) > 0.97 && player.hasLineOfSight(mob)) {
                Gates.force(mob, level.damageSources().magic(), 2.0f);
                level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getEyeY(), mob.getZ(),
                        6, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }
}
