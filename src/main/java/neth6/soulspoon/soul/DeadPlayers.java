package neth6.soulspoon.soul;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DeadPlayers extends SavedData {
    private static final String KEY = "soulspoon_dead_players";

    // UUID del jugador -> su nombre
    private final Map<UUID, String> dead = new LinkedHashMap<>();

    public static DeadPlayers get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        DeadPlayers::new,
                        DeadPlayers::load,
                        DataFixTypes.LEVEL
                ),
                KEY
        );
    }

    public boolean isDead(UUID id) {
        return dead.containsKey(id);
    }

    public void markDead(UUID id, String name) {
        dead.put(id, name);
        setDirty();
    }

    public boolean revive(UUID id) {
        boolean removed = dead.remove(id) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public Map<UUID, String> getAll() {
        return Collections.unmodifiableMap(dead);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, String> entry : dead.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putUUID("id", entry.getKey());
            item.putString("name", entry.getValue());
            list.add(item);
        }
        tag.put("dead", list);
        return tag;
    }

    public static DeadPlayers load(CompoundTag tag, HolderLookup.Provider registries) {
        DeadPlayers data = new DeadPlayers();
        ListTag list = tag.getList("dead", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            data.dead.put(item.getUUID("id"), item.getString("name"));
        }
        return data;
    }
}