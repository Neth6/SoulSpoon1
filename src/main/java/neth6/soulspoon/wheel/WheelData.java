package neth6.soulspoon.wheel;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class WheelData extends SavedData {
    private static final String KEY = "soulspoon_wheel";

    private final Set<WheelColor> disabled = EnumSet.noneOf(WheelColor.class);

    public static WheelData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        WheelData::new,
                        WheelData::load,
                        DataFixTypes.LEVEL
                ),
                KEY
        );
    }

    public boolean isOn(WheelColor color) {
        return !disabled.contains(color);
    }

    public void set(WheelColor color, boolean on) {
        boolean changed = on ? disabled.remove(color) : disabled.add(color);
        if (changed) {
            setDirty();
        }
    }

    public List<WheelColor> enabledColors() {
        List<WheelColor> result = new ArrayList<>();
        for (WheelColor color : WheelColor.values()) {
            if (isOn(color)) {
                result.add(color);
            }
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (WheelColor color : disabled) {
            list.add(StringTag.valueOf(color.id()));
        }
        tag.put("off", list);
        return tag;
    }

    public static WheelData load(CompoundTag tag, HolderLookup.Provider registries) {
        WheelData data = new WheelData();
        ListTag list = tag.getList("off", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            WheelColor color = WheelColor.byId(list.getString(i));
            if (color != null) {
                data.disabled.add(color);
            }
        }
        return data;
    }
}