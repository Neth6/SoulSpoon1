package neth6.soulspoon.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

// Parte 5: mobs extra del Centro de Quiu segun el documento V2.
public final class Variants5 {
    private Variants5() {
    }

    private static boolean done = false;

    public static void defineAll() {
        if (done) {
            return;
        }
        done = true;

        // Dromoraptor Verde: sabana de Quiu, manada, Veneno II por 2 segundos.
        Variants.register(new Variant("dromoraptor_verde", "Dromoraptor Verde", EntityType.WOLF)
                .setup(mob -> {
                    set(mob, Attributes.MOVEMENT_SPEED, 0.5);
                    set(mob, Attributes.ATTACK_DAMAGE, 6.0);
                })
                .tick((mob, level) -> hunt(mob, level))
                .hit((mob, player) -> player.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 1))));
    }

    private static void set(Mob mob, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                            double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static void hunt(Mob mob, ServerLevel level) {
        if (mob.getTarget() == null) {
            Player player = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), 32,
                    EntitySelector.NO_CREATIVE_OR_SPECTATOR);
            if (player != null) {
                mob.setTarget(player);
            }
        }
    }
}
