package neth6.soulspoon.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

// Estado de la grieta de acceso al Centro de Quiu (abierta o no).
public class RiftData extends SavedData {
    private static final String KEY = "soulspoon_rift";
    private boolean open;

    public static RiftData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(RiftData::new, RiftData::load, DataFixTypes.LEVEL), KEY);
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean value) {
        this.open = value;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("open", open);
        return tag;
    }

    public static RiftData load(CompoundTag tag, HolderLookup.Provider registries) {
        RiftData data = new RiftData();
        data.open = tag.getBoolean("open");
        return data;
    }
}
