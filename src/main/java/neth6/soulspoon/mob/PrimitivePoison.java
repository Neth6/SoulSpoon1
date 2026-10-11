package neth6.soulspoon.mob;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// "Veneno Primitivo": mientras dura, el jugador no puede curarse y sus totems no funcionan.
public class PrimitivePoison {
    private static final Map<UUID, Long> UNTIL = new HashMap<>();
    private static final Map<UUID, Float> LAST_HEALTH = new HashMap<>();

    public static void apply(ServerPlayer player, int ticks) {
        long end = player.server.getTickCount() + ticks;
        UNTIL.merge(player.getUUID(), end, Math::max);
        LAST_HEALTH.put(player.getUUID(), player.getHealth());
        player.addEffect(new MobEffectInstance(MobEffects.POISON, Math.min(ticks, 100), 0));
    }

    public static boolean isPoisoned(ServerPlayer player) {
        Long end = UNTIL.get(player.getUUID());
        return end != null && end > player.server.getTickCount();
    }

    public static void initialize() {
        // Sin curacion: si la vida sube mientras dura el veneno, se vuelve a bajar.
        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
            if (UNTIL.isEmpty()) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!isPoisoned(player)) {
                    UNTIL.remove(player.getUUID());
                    LAST_HEALTH.remove(player.getUUID());
                    continue;
                }
                float last = LAST_HEALTH.getOrDefault(player.getUUID(), player.getHealth());
                if (player.getHealth() > last) {
                    player.setHealth(last);
                }
                LAST_HEALTH.put(player.getUUID(), player.getHealth());
            }
        });
        // Sin totems: un golpe que mataria al jugador envenenado se lleva sus totems antes.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player && isPoisoned(player)
                    && player.getHealth() - amount <= 0) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.is(Items.TOTEM_OF_UNDYING)) {
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
            }
            return true;
        });
    }
}
