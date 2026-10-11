package neth6.soulspoon.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Gates {
    private Gates() {}

    public static final String ALTAVOZ = "soulspoon:altavoz";
    public static final String LINTERNA = "soulspoon:linterna";

    // true = el golpe se ignora.
    public static boolean blocks(Mob mob, DamageSource source) {
        if (!(mob instanceof Gated gated) || gated.gateOpen()) {
            return false;
        }
        // /kill, el vacio y el modo creativo siempre funcionan.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        // Ojo Volador: solo la luz de la Linterna lo daña (se aplica desde Variants4).
        if (mob.getTags().contains("ojo_volador")) {
            return true;
        }
        // Creeper Visceral: solo muere con el Altavoz en la mano.
        if (mob.getTags().contains("creeper_visceral")) {
            return !(source.getEntity() instanceof Player player && holds(player, ALTAVOZ));
        }
        return false;
    }

    public static boolean holds(Player player, String itemId) {
        return isItem(player.getMainHandItem(), itemId) || isItem(player.getOffhandItem(), itemId);
    }

    private static boolean isItem(ItemStack stack, String itemId) {
        return !stack.isEmpty()
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId);
    }

    // Daño que salta la protección (luz de la linterna, etc.).
    public static void force(Mob mob, DamageSource source, float amount) {
        if (mob instanceof Gated gated) {
            gated.setGateOpen(true);
            try {
                mob.hurt(source, amount);
            } finally {
                gated.setGateOpen(false);
            }
        } else {
            mob.hurt(source, amount);
        }
    }
}
