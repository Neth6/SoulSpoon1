package neth6.soulspoon.soul;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

import neth6.soulspoon.SoulSpoon;

public class SoulManager {
    public static final int MAX_SOULS = 3;

    // Dato "almas" pegado a cada jugador: empieza en 3, se guarda en el
    // mundo y se conserva al morir.
    public static final AttachmentType<Integer> SOULS = AttachmentRegistry.create(
            SoulSpoon.id("souls"),
            builder -> builder
                    .initializer(() -> MAX_SOULS)
                    .persistent(Codec.INT)
                    .copyOnDeath()
    );

    public static int getSouls(Player player) {
        return player.getAttachedOrCreate(SOULS);
    }

    // Gasta 1 alma. Devuelve false si el jugador ya no tiene.
    public static boolean spendSoul(Player player) {
        int souls = getSouls(player);
        if (souls <= 0) {
            return false;
        }
        player.setAttached(SOULS, souls - 1);
        return true;
    }

    // Llamarlo al iniciar el mod hace que Java cargue esta clase y se registre el dato.
    public static void initialize() {
    }
}