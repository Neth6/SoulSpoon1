package neth6.soulspoon.soul;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class AltarData extends SavedData {
    private static final String KEY = "soulspoon_altar";

    private boolean hasAltar = false;
    private long pos = 0L;

    public static AltarData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        AltarData::new,
                        AltarData::load,
                        DataFixTypes.LEVEL
                ),
                KEY
        );
    }

    public boolean hasAltar() {
        return hasAltar;
    }

    public BlockPos getAltarPos() {
        return BlockPos.of(pos);
    }

    public void setAltar(BlockPos altarPos) {
        hasAltar = true;
        pos = altarPos.asLong();
        setDirty();
    }

    // Solo borra el registro si el altar quitado es el que estaba registrado.
    public void clearAltar(BlockPos altarPos) {
        if (hasAltar && pos == altarPos.asLong()) {
            hasAltar = false;
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("has", hasAltar);
        tag.putLong("pos", pos);
        return tag;
    }

    public static AltarData load(CompoundTag tag, HolderLookup.Provider registries) {
        AltarData data = new AltarData();
        data.hasAltar = tag.getBoolean("has");
        data.pos = tag.getLong("pos");
        return data;
    }
}