package neth6.soulspoon.economy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Saldo de deditas de cada jugador (guardado en el mundo). */
public class EconomyData extends SavedData {
    private static final String KEY = "soulspoon_economy";

    private final Map<UUID, Long> balances = new HashMap<>();

    public static EconomyData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(EconomyData::new, EconomyData::load, DataFixTypes.LEVEL),
                KEY);
    }

    public long balance(UUID player) {
        return balances.getOrDefault(player, 0L);
    }

    public void set(UUID player, long value) {
        balances.put(player, Math.max(0L, value));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        balances.forEach((id, value) -> all.putLong(id.toString(), value));
        tag.put("balances", all);
        return tag;
    }

    public static EconomyData load(CompoundTag tag, HolderLookup.Provider registries) {
        EconomyData data = new EconomyData();
        CompoundTag all = tag.getCompound("balances");
        for (String key : all.getAllKeys()) {
            try {
                data.balances.put(UUID.fromString(key), all.getLong(key));
            } catch (IllegalArgumentException ignored) {
                // clave rara: se ignora
            }
        }
        return data;
    }
}
