package neth6.soulspoon.soul;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

public class ReviveManager {
    public enum Result {
        SUCCESS,
        NO_SOULS,
        NOT_DEAD
    }

    // El jugador "reviver" gasta 1 alma para revivir al jugador llamado targetName.
    public static Result revive(ServerPlayer reviver, String targetName) {
        DeadPlayers dead = DeadPlayers.get(reviver.server);

        UUID targetId = null;
        for (Map.Entry<UUID, String> entry : dead.getAll().entrySet()) {
            if (entry.getValue().equalsIgnoreCase(targetName)) {
                targetId = entry.getKey();
                break;
            }
        }

        if (targetId == null) {
            return Result.NOT_DEAD;
        }
        if (!SoulManager.spendSoul(reviver)) {
            return Result.NO_SOULS;
        }

        dead.revive(targetId);
        return Result.SUCCESS;
    }
}