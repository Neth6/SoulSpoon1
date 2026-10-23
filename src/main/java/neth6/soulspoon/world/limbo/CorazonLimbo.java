package neth6.soulspoon.world.limbo;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Corazon del Limbo: al usarlo devuelve corazones (cura 4 corazones y da un rato de regeneracion).
public class CorazonLimbo extends Item {
    public static final float HEAL = 8.0f; // 4 corazones

    public CorazonLimbo(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getHealth() >= player.getMaxHealth()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level instanceof ServerLevel server) {
            player.heal(HEAL);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            server.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(),
                    8, 0.4, 0.5, 0.4, 0.0);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 0.6f, 1.6f);
        }
        player.getCooldowns().addCooldown(this, 20);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
