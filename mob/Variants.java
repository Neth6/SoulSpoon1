package neth6.soulspoon.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.util.Cmd;
import neth6.soulspoon.world.EventManager;
import neth6.soulspoon.world.WorldRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Parte 3: todos los mobs "variante" del DEDSAFIO sobre mobs vanilla.
// Los textos con tilde usan \u para evitar problemas de codificacion en Windows.
public final class Variants {
    public static final List<Variant> ALL = new ArrayList<>();
    private static final Map<String, Variant> BY_TAG = new HashMap<>();
    private static final Map<String, Variant> BY_ID = new HashMap<>();

    private static final ResourceLocation BLACK_GHOST = SoulSpoon.id("fantasma_negro");

    private Variants() {
    }

    public static Variant byTag(String tag) {
        return BY_TAG.get(tag);
    }

    public static Variant byId(String id) {
        return BY_ID.get(id);
    }

    private static Variant add(Variant variant) {
        ALL.add(variant);
        BY_TAG.put(variant.tag(), variant);
        BY_ID.put(variant.id(), variant);
        return variant;
    }

    // ---------- Utilidades ----------

    private static void attr(Mob mob, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static void fireProof(Mob mob) {
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                MobEffectInstance.INFINITE_DURATION, 0, false, false));
    }

    private static Player nearestPlayer(Mob mob, ServerLevel level, double range) {
        return level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), range,
                EntitySelector.NO_CREATIVE_OR_SPECTATOR);
    }

    // El mob persigue al jugador mas cercano aunque no lo haya visto.
    private static void hunt(Mob mob, ServerLevel level, double range) {
        if (mob.getTarget() == null) {
            Player player = nearestPlayer(mob, level, range);
            if (player != null) {
                mob.setTarget(player);
            }
        }
    }

    private static void poison(ServerPlayer player) {
        // "Veneno Primitivo"
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
    }

    // Se "entierra": invisible hasta que un jugador se acerca.
    private static void bury(Mob mob, ServerLevel level) {
        boolean near = nearestPlayer(mob, level, 8) != null;
        mob.setInvisible(!near);
        if (near) {
            hunt(mob, level, 16);
        }
    }

    private static void raptorTick(Mob mob, ServerLevel level, double baseDamage) {
        hunt(mob, level, 32);
        boolean cherry = level.getBiome(mob.blockPosition()).is(Biomes.CHERRY_GROVE);
        attr(mob, Attributes.ATTACK_DAMAGE, baseDamage + (cherry ? 7 : 0)); // [48]
    }

    private static Variant raptor(String id, String name, double speed, double damage) {
        return add(new Variant(id, name, EntityType.WOLF)
                .setup(mob -> {
                    attr(mob, Attributes.MOVEMENT_SPEED, speed);
                    attr(mob, Attributes.ATTACK_DAMAGE, damage);
                })
                .tick((mob, level) -> raptorTick(mob, level, damage)));
    }

    private static boolean overworld(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD;
    }

    private static boolean nether(ServerLevel level) {
        return level.dimension() == Level.NETHER;
    }

    private static List<ServerPlayer> playersNear(Mob mob, ServerLevel level, double range) {
        return level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(range));
    }

    private static void phantomSetup(Mob mob) {
        fireProof(mob); // los phantoms no se queman con el sol
    }

    public static void restoreHearts(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance != null) {
            instance.removeModifier(BLACK_GHOST);
        }
    }

    // ---------- 3a: Creepers ----------

    static {
        // [14][70] Creeper Nuclear: explosion de radio 25; aparece en el techo del Nether.
        add(new Variant("creeper_nuclear", "Creeper Nuclear", EntityType.CREEPER)
                .setup(mob -> Cmd.merge(mob, "{ExplosionRadius:25b}"))
                .spawn(Variant.Place.GROUND, (p, l) -> nether(l) && p.getY() >= 120));

        // [26] Creeper de Pasto: pequeno, rapido y mecha corta.
        add(new Variant("creeper_pasto", "Creeper de Pasto", EntityType.CREEPER)
                .setup(mob -> {
                    attr(mob, Attributes.SCALE, 0.5);
                    attr(mob, Attributes.MOVEMENT_SPEED, 0.5);
                    Cmd.merge(mob, "{Fuse:10s}");
                }));

        // [31] Creeper de Magma: al explotar deja lava.
        add(new Variant("creeper_magma", "Creeper de Magma", EntityType.CREEPER)
                .end((mob, level) -> {
                    if (mob instanceof Creeper creeper && creeper.getSwellDir() > 0) {
                        BlockPos pos = mob.blockPosition();
                        if (level.getBlockState(pos).canBeReplaced()) {
                            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
                        }
                    }
                }));

        // [40][56][71] Creeper Raiz de Azalea: atrapa con raices (escape con WASD).
        add(new Variant("creeper_azalea", "Creeper Ra\u00edz de Azalea", EntityType.CREEPER)
                .tick((mob, level) -> {
                    Player player = nearestPlayer(mob, level, 3);
                    if (player instanceof ServerPlayer target && !Roots.isRooted(target)) {
                        Roots.root(target);
                        level.sendParticles(ParticleTypes.COMPOSTER, target.getX(), target.getY() + 0.5,
                                target.getZ(), 20, 0.4, 0.4, 0.4, 0.0);
                        mob.discard();
                    }
                })
                .spawn(Variant.Place.GROUND, (p, l) -> overworld(l) && l.isDay()
                        && l.getBiome(p.blockPosition()).is(Biomes.PLAINS)));

        // [46] Creeper Rojo: efectos alterados (aqui: efectos negativos al azar al explotar).
        add(new Variant("creeper_rojo", "Creeper Rojo", EntityType.CREEPER)
                .end((mob, level) -> {
                    if (!(mob instanceof Creeper creeper) || creeper.getSwellDir() <= 0) {
                        return;
                    }
                    for (ServerPlayer player : playersNear(mob, level, 7)) {
                        switch (level.random.nextInt(5)) {
                            case 0 -> player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
                            case 1 -> player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                            case 2 -> player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 2));
                            case 3 -> player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 0));
                            default -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 2));
                        }
                    }
                }));

        // [47] Creeper de Rosa de Wither: aplica Wither; aparece en el Nether.
        add(new Variant("creeper_rosa", "Creeper de Rosa de Wither", EntityType.CREEPER)
                .end((mob, level) -> {
                    if (!(mob instanceof Creeper creeper) || creeper.getSwellDir() <= 0) {
                        return;
                    }
                    for (ServerPlayer player : playersNear(mob, level, 7)) {
                        player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
                    }
                    BlockPos pos = mob.blockPosition();
                    BlockState rose = Blocks.WITHER_ROSE.defaultBlockState();
                    if (level.getBlockState(pos).canBeReplaced() && rose.canSurvive(level, pos)) {
                        level.setBlock(pos, rose, 3);
                    }
                })
                .spawn(Variant.Place.GROUND, (p, l) -> nether(l)));

        // [54] Creeper Resorte: lanza a los jugadores a gran altura; sale de dia.
        add(new Variant("creeper_resorte", "Creeper Resorte", EntityType.CREEPER)
                .setup(mob -> Cmd.merge(mob, "{ExplosionRadius:1b}"))
                .end((mob, level) -> {
                    if (!(mob instanceof Creeper creeper) || creeper.getSwellDir() <= 0) {
                        return;
                    }
                    for (ServerPlayer player : playersNear(mob, level, 8)) {
                        player.setDeltaMovement(player.getDeltaMovement().add(0.0, 2.4, 0.0));
                        player.hurtMarked = true;
                    }
                })
                .spawn(Variant.Place.GROUND, (p, l) -> overworld(l) && l.isDay()));
    }

    // ---------- 3b: Reptisaurios y dinosaurios ----------

    static {
        // [15] Reptisaurio Salvaje (emboscador: se entierra).
        add(new Variant("reptisaurio_salvaje", "Reptisaurio Salvaje", EntityType.RAVAGER)
                .setup(mob -> attr(mob, Attributes.MOVEMENT_SPEED, 0.4))
                .tick(Variants::bury));

        // [32] Reptisaurio Emboscador.
        add(new Variant("reptisaurio_emboscador", "Reptisaurio Emboscador", EntityType.SILVERFISH)
                .setup(mob -> attr(mob, Attributes.ATTACK_DAMAGE, 8))
                .tick(Variants::bury));

        // [16] Plumosaurio: pacifico hasta que le hacen dano.
        add(new Variant("plumosaurio", "Plumosaurio", EntityType.IRON_GOLEM)
                .setup(mob -> attr(mob, Attributes.ATTACK_DAMAGE, 40))
                .hurt((mob, attacker) -> {
                    if (attacker instanceof Player player && !player.isCreative()) {
                        mob.setTarget(attacker);
                    }
                }));

        // [17][37] Dromoraptores (manadas de lobos agresivos).
        raptor("dromoraptor_rojo", "Dromoraptor Rojo", 0.45, 12);
        raptor("dromoraptor_azul", "Dromoraptor Azul", 0.65, 4);

        // [18][19][20] Reptisaurios tribales: aplican Veneno Primitivo al golpear.
        add(new Variant("reptisaurio_guerrero", "Reptisaurio Guerrero", EntityType.PIGLIN_BRUTE)
                .setup(mob -> {
                    if (mob instanceof PiglinBrute brute) {
                        brute.setImmuneToZombification(true);
                    }
                })
                .hit((mob, player) -> poison(player)));
        add(new Variant("reptisaurio_arquero", "Reptisaurio Arquero", EntityType.SKELETON)
                .setup(Variants::fireProof)
                .hit((mob, player) -> poison(player)));
        add(new Variant("reptisaurio_lanza", "Reptisaurio con Lanza", EntityType.DROWNED)
                .setup(mob -> mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT)))
                .hit((mob, player) -> poison(player)));

        // [30][69] Dactylo Bebe: vuela y dispara proyectiles de Ceguera; sale en pantanos.
        add(new Variant("dactylo_bebe", "D\u00e1ctylo Beb\u00e9", EntityType.PHANTOM)
                .setup(mob -> {
                    phantomSetup(mob);
                    attr(mob, Attributes.SCALE, 0.5);
                })
                .tick((mob, level) -> {
                    hunt(mob, level, 24);
                    if (mob.getTarget() instanceof ServerPlayer player && mob.tickCount % 60 < 5
                            && mob.distanceTo(player) < 18 && mob.hasLineOfSight(player)) {
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                        Vec3 from = mob.position();
                        Vec3 to = player.getEyePosition();
                        for (int i = 1; i <= 8; i++) {
                            Vec3 point = from.add(to.subtract(from).scale(i / 9.0));
                            level.sendParticles(ParticleTypes.SQUID_INK, point.x, point.y, point.z,
                                    1, 0.0, 0.0, 0.0, 0.0);
                        }
                    }
                })
                .spawn(Variant.Place.AIR, (p, l) -> overworld(l)
                        && (l.getBiome(p.blockPosition()).is(Biomes.SWAMP)
                        || l.getBiome(p.blockPosition()).is(Biomes.MANGROVE_SWAMP))));

        // [27][35] Narval: embestidas acuaticas; migra a los oceanos.
        add(new Variant("narval", "Narval", EntityType.DOLPHIN)
                .setup(mob -> attr(mob, Attributes.ATTACK_DAMAGE, 10))
                .tick((mob, level) -> {
                    Player player = nearestPlayer(mob, level, 20);
                    if (player != null && player.isInWater() && mob.getTarget() == null) {
                        mob.setTarget(player);
                    }
                })
                .spawn(Variant.Place.WATER, (p, l) -> p.isInWater()
                        && l.getBiome(p.blockPosition()).is(BiomeTags.IS_OCEAN)));
    }

    // ---------- 3c: Resto ----------

    static {
        // [3] Bomba Warden: phantom que explota al llegar al objetivo.
        add(new Variant("bomba_warden", "Bomba Warden", EntityType.PHANTOM)
                .setup(mob -> {
                    phantomSetup(mob);
                    attr(mob, Attributes.MAX_HEALTH, 10);
                })
                .tick((mob, level) -> {
                    hunt(mob, level, 32);
                    LivingEntity target = mob.getTarget();
                    if (target != null && mob.distanceTo(target) < 2.5f) {
                        level.explode(mob, mob.getX(), mob.getY(), mob.getZ(), 3.0f,
                                Level.ExplosionInteraction.MOB);
                        mob.discard();
                    }
                }));

        // [6] Pod Phora y [7] Skiff Phora (el x4 de dano [43] es la regla podskiffx4).
        add(new Variant("pod_phora", "Pod Phora", EntityType.GHAST)
                .setup(mob -> attr(mob, Attributes.MAX_HEALTH, 40)));
        add(new Variant("skiff_phora", "Skiff Phora", EntityType.PHANTOM)
                .setup(mob -> {
                    phantomSetup(mob);
                    attr(mob, Attributes.ATTACK_DAMAGE, 12);
                })
                .tick((mob, level) -> hunt(mob, level, 32)));

        // [13] Blazes en todo el Nether / [52] Wither Skeletons en todo el Nether.
        add(new Variant("blaze_nether", null, EntityType.BLAZE)
                .spawn(Variant.Place.GROUND, (p, l) -> nether(l)));
        add(new Variant("wither_nether", null, EntityType.WITHER_SKELETON)
                .spawn(Variant.Place.GROUND, (p, l) -> nether(l)));

        // [34] Herobrine.
        add(new Variant("herobrine", "Herobrine", EntityType.ZOMBIE)
                .setup(mob -> {
                    attr(mob, Attributes.MOVEMENT_SPEED, 0.45);
                    attr(mob, Attributes.ATTACK_DAMAGE, 20);
                }));

        // [41] Zombie Plata (rapido; trepa con la regla trepadores).
        add(new Variant("zombie_plata", "Zombie Plata", EntityType.ZOMBIE)
                .setup(mob -> attr(mob, Attributes.MOVEMENT_SPEED, 0.45)));

        // [55] Zombie Planta: aparece en el bioma Forest.
        add(new Variant("zombie_planta", "Zombie Planta", EntityType.ZOMBIE)
                .spawn(Variant.Place.GROUND, (p, l) -> overworld(l)
                        && l.getBiome(p.blockPosition()).is(Biomes.FOREST)));

        // [88] Zombie Minero: pico de diamante; con la Luna (evento luna) pica bloques.
        add(new Variant("zombie_minero", "Zombie Minero", EntityType.ZOMBIE)
                .setup(mob -> mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE)))
                .tick((mob, level) -> {
                    if (!EventManager.isActive(EventManager.WorldEvent.LUNA) || !mob.horizontalCollision) {
                        return;
                    }
                    BlockPos front = mob.blockPosition().relative(mob.getDirection());
                    for (BlockPos pos : new BlockPos[]{front, front.above()}) {
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0) {
                            level.destroyBlock(pos, true, mob);
                        }
                    }
                })
                .spawn(Variant.Place.GROUND, (p, l) -> overworld(l) && l.isNight()));

        // [58] Aranas de cueva de noche en el Overworld.
        add(new Variant("arana_cueva_noche", null, EntityType.CAVE_SPIDER)
                .spawn(Variant.Place.GROUND, (p, l) -> overworld(l) && l.isNight()));

        // [73] Fantasma Negro: quita 1 corazon permanente por golpe.
        add(new Variant("fantasma_negro", "Fantasma Negro", EntityType.PHANTOM)
                .setup(Variants::phantomSetup)
                .tick((mob, level) -> hunt(mob, level, 32))
                .hit((mob, player) -> {
                    AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
                    if (health == null) {
                        return;
                    }
                    AttributeModifier old = health.getModifier(BLACK_GHOST);
                    double current = old == null ? 0.0 : old.amount();
                    if (current <= -18.0) {
                        return; // siempre queda al menos 1 corazon
                    }
                    health.removeModifier(BLACK_GHOST);
                    health.addPermanentModifier(new AttributeModifier(
                            BLACK_GHOST, current - 2.0, AttributeModifier.Operation.ADD_VALUE));
                    if (player.getHealth() > player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                }));

        // [76] Fantasma Blanco: Lentitud.
        add(new Variant("fantasma_blanco", "Fantasma Blanco", EntityType.PHANTOM)
                .setup(Variants::phantomSetup)
                .tick((mob, level) -> hunt(mob, level, 32))
                .hit((mob, player) -> player.addEffect(
                        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1))));

        // [75] Vindicador de las Torres del Limbo: deshabilita escudos, dropea Manzana de Oro.
        add(new Variant("vindicador_limbo", "Vindicador de las Torres del Limbo", EntityType.VINDICATOR)
                .hit((mob, player) -> {
                    player.getCooldowns().addCooldown(Items.SHIELD, 200);
                    player.stopUsingItem();
                })
                .death(mob -> mob.spawnAtLocation(Items.GOLDEN_APPLE)));

        // [86] Phantom Oscuro: con insomnia alta suelta un slot completo del inventario.
        add(new Variant("fantasma_oscuro", "Phantom Oscuro", EntityType.PHANTOM)
                .setup(Variants::phantomSetup)
                .tick((mob, level) -> hunt(mob, level, 32))
                .hit((mob, player) -> {
                    if (!WorldRules.isInsomniaHigh(player)) {
                        return;
                    }
                    List<Integer> slots = new ArrayList<>();
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        if (!player.getInventory().getItem(i).isEmpty()) {
                            slots.add(i);
                        }
                    }
                    if (slots.isEmpty()) {
                        return;
                    }
                    int slot = slots.get(player.getRandom().nextInt(slots.size()));
                    ItemStack stack = player.getInventory().getItem(slot);
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                    player.drop(stack, true);
                })
                .spawn(Variant.Place.AIR, (p, l) -> overworld(l) && WorldRules.isInsomniaHigh(p)));

        // [87] Phantom Explosivo: con insomnia alta genera TNT al impactar.
        add(new Variant("fantasma_explosivo", "Phantom Explosivo", EntityType.PHANTOM)
                .setup(Variants::phantomSetup)
                .tick((mob, level) -> hunt(mob, level, 32))
                .hit((mob, player) -> {
                    if (WorldRules.isInsomniaHigh(player) && player.level() instanceof ServerLevel level) {
                        Cmd.run(level, player.position(), "summon minecraft:tnt "
                                + player.getX() + " " + player.getY() + " " + player.getZ()
                                + " {fuse:40}");
                    }
                })
                .spawn(Variant.Place.AIR, (p, l) -> overworld(l) && WorldRules.isInsomniaHigh(p)));
    }
}
