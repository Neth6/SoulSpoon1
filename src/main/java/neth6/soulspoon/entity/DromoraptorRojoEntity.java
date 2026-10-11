package neth6.soulspoon.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Dromoraptor Rojo: cazador veloz de manada. Cuando uno fija un objetivo, los de alrededor también. */
public class DromoraptorRojoEntity extends Monster implements GeoEntity {
    private static final double PACK_RANGE = 20.0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.dromoraptor_rojo.idle");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.dromoraptor_rojo.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DromoraptorRojoEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(DromoraptorRojoEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Caza coordinada: al fijar objetivo, avisa a los de su manada que aún no tengan uno. */
    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target);
        if (target == null || level().isClientSide) return;
        AABB area = getBoundingBox().inflate(PACK_RANGE);
        for (DromoraptorRojoEntity mate : level().getEntitiesOfClass(DromoraptorRojoEntity.class, area)) {
            if (mate != this && mate.getTarget() == null) mate.setTarget(target);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 3,
                state -> state.setAndContinue(state.isMoving() ? RUN : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
