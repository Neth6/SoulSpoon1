package neth6.soulspoon.world.limbo;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Datos del Limbo guardados en el mundo: chunks ya decorados y cofres con su proximo rellenado.
public class LimboData extends SavedData {
    private static final String KEY = "soulspoon_limbo";

    private final Set<Long> decorated = new HashSet<>();
    // posicion del cofre (BlockPos.asLong) -> tick de juego en que se rellena de nuevo
    private final Map<Long, Long> chests = new HashMap<>();

    public static LimboData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(LimboData::new, LimboData::load, DataFixTypes.LEVEL), KEY);
    }

    public boolean isDecorated(long chunk) {
        return decorated.contains(chunk);
    }

    public void markDecorated(long chunk) {
        decorated.add(chunk);
        setDirty();
    }

    public Map<Long, Long> chests() {
        return chests;
    }

    public void addChest(long pos, long nextRefill) {
        chests.put(pos, nextRefill);
        setDirty();
    }

    public void removeChest(long pos) {
        chests.remove(pos);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        long[] done = new long[decorated.size()];
        int i = 0;
        for (long chunk : decorated) {
            done[i++] = chunk;
        }
        tag.putLongArray("decorated", done);

        ListTag list = new ListTag();
        for (Map.Entry<Long, Long> entry : chests.entrySet()) {
            CompoundTag chest = new CompoundTag();
            chest.putLong("pos", entry.getKey());
            chest.putLong("next", entry.getValue());
            list.add(chest);
        }
        tag.put("chests", list);
        return tag;
    }

    public static LimboData load(CompoundTag tag, HolderLookup.Provider registries) {
        LimboData data = new LimboData();
        for (long chunk : tag.getLongArray("decorated")) {
            data.decorated.add(chunk);
        }
        ListTag list = tag.getList("chests", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag chest = list.getCompound(i);
            data.chests.put(chest.getLong("pos"), chest.getLong("next"));
        }
        return data;
    }
}
