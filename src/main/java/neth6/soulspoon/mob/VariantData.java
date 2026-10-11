package neth6.soulspoon.mob;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

// Que variantes tienen aparicion natural activada.
public class VariantData extends SavedData {
    private static final String KEY = "soulspoon_variants";

    private final Set<String> enabled = new HashSet<>();

    public static VariantData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VariantData::new, VariantData::load, DataFixTypes.LEVEL),
                KEY);
    }

    public boolean isOn(String id) {
        return enabled.contains(id);
    }

    public void set(String id, boolean on) {
        boolean changed = on ? enabled.add(id) : enabled.remove(id);
        if (changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (String id : enabled) {
            list.add(StringTag.valueOf(id));
        }
        tag.put("on", list);
        return tag;
    }

    public static VariantData load(CompoundTag tag, HolderLookup.Provider registries) {
        VariantData data = new VariantData();
        ListTag list = tag.getList("on", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            data.enabled.add(list.getString(i));
        }
        return data;
    }
}
