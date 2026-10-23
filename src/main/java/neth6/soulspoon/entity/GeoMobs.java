package neth6.soulspoon.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import neth6.soulspoon.SoulSpoon;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Mobs con modelo de Blockbench (GeckoLib) que reemplazan a los "vanilla con etiqueta" de Variants.
 * Cada clase hereda del mob vanilla para conservar su IA; solo cambia el modelo.
 * Archivos por mob: geo/entity/<id>.geo.json, animations/entity/<id>.animation.json,
 * textures/entity/<id>.png (los genera tools/importar_bbmodel.py).
 */
public final class GeoMobs {
    private GeoMobs() {}

    // ---------------------------------------------------------------- clases

    public static class GeoCreeper extends Creeper implements GeoEntity {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        public GeoCreeper(EntityType<? extends Creeper> type, Level level) {
            super(type, level);
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            GeoAnim.move(this, controllers);
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }

    public static class GeoZombie extends Zombie implements GeoEntity {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        public GeoZombie(EntityType<? extends Zombie> type, Level level) {
            super(type, level);
        }

        @Override
        protected boolean isSunSensitive() {
            return false; // Herobrine no se quema con el sol.
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            GeoAnim.move(this, controllers);
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }

    public static class GeoPhantom extends Phantom implements GeoEntity {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        public GeoPhantom(EntityType<? extends Phantom> type, Level level) {
            super(type, level);
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            GeoAnim.move(this, controllers);
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }

    public static class GeoRavager extends Ravager implements GeoEntity {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        public GeoRavager(EntityType<? extends Ravager> type, Level level) {
            super(type, level);
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            GeoAnim.move(this, controllers);
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }

    public static class GeoDolphin extends Dolphin implements GeoEntity {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        public GeoDolphin(EntityType<? extends Dolphin> type, Level level) {
            super(type, level);
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            GeoAnim.move(this, controllers);
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }

    // ---------------------------------------------------------------- tipos
    // ids = nombre de los archivos del modelo y etiqueta de la Variant.

    public static final EntityType<GeoCreeper> CREEPER_PASTO = register("creeper_pasto",
            EntityType.Builder.<GeoCreeper>of(GeoCreeper::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.1f).clientTrackingRange(8),
            Creeper.createAttributes());

    public static final EntityType<GeoCreeper> CREEPER_PASTEL = register("creeper_pastel",
            EntityType.Builder.<GeoCreeper>of(GeoCreeper::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f).clientTrackingRange(8),
            Creeper.createAttributes());

    public static final EntityType<GeoZombie> HEROBRINE = register("herobrine",
            EntityType.Builder.<GeoZombie>of(GeoZombie::new, MobCategory.MONSTER)
                    .sized(0.6f, 2.0f).clientTrackingRange(8),
            Zombie.createAttributes());

    public static final EntityType<GeoPhantom> FANTASMA_NEGRO = register("fantasma_negro",
            EntityType.Builder.<GeoPhantom>of(GeoPhantom::new, MobCategory.MONSTER)
                    .sized(0.7f, 1.2f).clientTrackingRange(8),
            Monster.createMonsterAttributes());

    public static final EntityType<GeoPhantom> FANTASMA_BLANCO = register("fantasma_blanco",
            EntityType.Builder.<GeoPhantom>of(GeoPhantom::new, MobCategory.MONSTER)
                    .sized(0.7f, 1.2f).clientTrackingRange(8),
            Monster.createMonsterAttributes());

    public static final EntityType<GeoRavager> REPTISAURIO_SALVAJE = register("reptisaurio_salvaje",
            EntityType.Builder.<GeoRavager>of(GeoRavager::new, MobCategory.MONSTER)
                    .sized(1.3f, 2.2f).clientTrackingRange(10),
            Ravager.createAttributes());

    public static final EntityType<GeoDolphin> NARVAL = register("narval",
            EntityType.Builder.<GeoDolphin>of(GeoDolphin::new, MobCategory.WATER_CREATURE)
                    .sized(1.2f, 1.0f).clientTrackingRange(10),
            Dolphin.createAttributes());

    // Mob nuevo (no estaba en la guia): Zarinosa Espigueya. Estadisticas provisionales.
    public static final EntityType<SoulMob> ZARINOSA_ESPIGUEYA = register("zarinosa_espigueya",
            EntityType.Builder.<SoulMob>of(SoulMob::new, MobCategory.MONSTER)
                    .sized(0.9f, 1.0f).clientTrackingRange(10),
            SoulMob.attributes(24.0, 6.0, 0.28));

    private static <T extends LivingEntity> EntityType<T> register(String id, EntityType.Builder<T> builder,
                                                                   AttributeSupplier.Builder attributes) {
        EntityType<T> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, SoulSpoon.id(id), builder.build(id));
        FabricDefaultAttributeRegistry.register(type, attributes);
        return type;
    }

    /** Fuerza la carga de la clase (y con ella el registro de todos los tipos). */
    public static void initialize() {
        SoulSpoon.LOGGER.info("GeoMobs: {} mobs con modelo Blockbench registrados", 8);
    }
}
