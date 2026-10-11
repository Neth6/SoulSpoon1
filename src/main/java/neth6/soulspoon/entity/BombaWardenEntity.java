package neth6.soulspoon.entity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * Bomba Warden: volador de la Deep Dark. Persigue al jugador; al acercarse a menos de 3 bloques
 * enciende una mecha corta (se oye el siseo del Creeper) y explota con potencia 3.
 * Si lo matas antes de que explote, no hay explosión.
 */
public class BombaWardenEntity extends Monster implements GeoEntity {
    public static final float EXPLOSION_POWER = 3.0F;
    public static final int FUSE_TICKS = 20;

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.bomba_warden.fly");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.bomba_warden.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int fuse = -1;

    public BombaWardenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FlyToTargetGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        LivingEntity target = getTarget();
        if (fuse < 0 && target != null && distanceToSqr(target) < 9.0) {
            fuse = 0;
            playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 1.2F);
        }
        if (fuse >= 0 && ++fuse >= FUSE_TICKS) {
            level().explode(this, getX(), getY(), getZ(), EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
            discard();
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 3,
                state -> state.setAndContinue(state.isMoving() ? FLY : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Vuela directo hacia el objetivo (a la altura de su cuerpo). */
    private static class FlyToTargetGoal extends Goal {
        private final BombaWardenEntity mob;

        FlyToTargetGoal(BombaWardenEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getMoveControl().setWantedPosition(t.getX(), t.getY(0.5), t.getZ(), 1.2);
            mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
        }
    }
}
