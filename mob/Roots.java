package neth6.soulspoon.mob;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// [40/71] Raices del Creeper Raiz de Azalea: el jugador queda inmovil y debe
// escapar pulsando una secuencia de WASD.
public class Roots {
    private static final String KEYS = "WASD";
    private static final int LENGTH = 4;
    private static final int TIMEOUT = 200; // 10 s

    private static class State {
        final int[] sequence = new int[LENGTH];
        int index;
        int ticksLeft = TIMEOUT;
        int previous;
    }

    private static final Map<UUID, State> ROOTED = new HashMap<>();

    public static boolean isRooted(ServerPlayer player) {
        return ROOTED.containsKey(player.getUUID());
    }

    public static void root(ServerPlayer player) {
        if (isRooted(player)) {
            return;
        }
        State state = new State();
        for (int i = 0; i < LENGTH; i++) {
            state.sequence[i] = player.getRandom().nextInt(4) + 1;
        }
        ROOTED.put(player.getUUID(), state);
    }

    // 1=W 2=A 3=S 4=D, 0 = ninguna
    private static int currentKey(ServerPlayer player) {
        if (player.zza > 0) {
            return 1;
        }
        if (player.zza < 0) {
            return 3;
        }
        if (player.xxa > 0) {
            return 2;
        }
        if (player.xxa < 0) {
            return 4;
        }
        return 0;
    }

    public static void tick(MinecraftServer server) {
        if (ROOTED.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, State>> it = ROOTED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, State> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            State state = entry.getValue();
            if (player == null || !player.isAlive()) {
                it.remove();
                continue;
            }
            // Inmovilizado: lentitud enorme y sin saltar.
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 10, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 128, true, false));

            int key = currentKey(player);
            if (key != 0 && state.previous == 0) {
                if (key == state.sequence[state.index]) {
                    state.index++;
                } else {
                    state.index = 0;
                }
            }
            state.previous = key;

            if (state.index >= LENGTH) {
                free(player);
                it.remove();
                continue;
            }
            if (--state.ticksLeft <= 0) {
                player.hurt(player.level().damageSources().generic(), 6.0f);
                free(player);
                it.remove();
                continue;
            }
            StringBuilder text = new StringBuilder("Raices! Pulsa: ");
            for (int i = 0; i < LENGTH; i++) {
                char letter = KEYS.charAt(state.sequence[i] - 1);
                text.append(i < state.index ? "[" + letter + "] " : letter + " ");
            }
            player.displayClientMessage(Component.literal(text.toString()), true);
        }
    }

    private static void free(ServerPlayer player) {
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.JUMP);
        player.displayClientMessage(Component.literal("Te liberaste"), true);
    }
}
