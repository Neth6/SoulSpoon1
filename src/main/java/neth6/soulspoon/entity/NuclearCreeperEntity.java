package neth6.soulspoon.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Creeper Nuclear: un Creeper normal (misma IA, mismo "hiss") con explosión enorme.
 * Hereda de Creeper para conservar todo su comportamiento y solo cambiamos radio y mecha.
 */
public class NuclearCreeperEntity extends Creeper implements GeoEntity {
    /** Radio base de la explosión. ¡OJO!: si está cargado por un rayo se DUPLICA. 25 puede dar lag. */
    public static final int EXPLOSION_RADIUS = 25;
    /** Mecha en ticks (el Creeper normal usa 30): más tiempo para huir. */
    public static final int FUSE_TICKS = 40;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.creeper_nuclear.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.creeper_nuclear.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NuclearCreeperEntity(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
        // "explosionRadius" y "maxSwell" son privados en Creeper; su NBT sí los acepta.
        CompoundTag tag = new CompoundTag();
        tag.putByte("ExplosionRadius", (byte) EXPLOSION_RADIUS);
        tag.putShort("Fuse", (short) FUSE_TICKS);
        this.readAdditionalSaveData(tag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 3,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
