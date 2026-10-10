package neth6.soulspoon.rule;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.EnumSet;
import java.util.Set;

public class RuleData extends SavedData {
    private static final String KEY = "soulspoon_rules";

    private final Set<Rule> enabled = EnumSet.noneOf(Rule.class);

    public static RuleData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        RuleData::new,
                        RuleData::load,
                        DataFixTypes.LEVEL
                ),
                KEY
        );
    }

    public boolean isOn(Rule rule) {
        return enabled.contains(rule);
    }

    public void set(Rule rule, boolean on) {
        boolean changed = on ? enabled.add(rule) : enabled.remove(rule);
        if (changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Rule rule : enabled) {
            list.add(StringTag.valueOf(rule.id()));
        }
        tag.put("on", list);
        return tag;
    }

    public static RuleData load(CompoundTag tag, HolderLookup.Provider registries) {
        RuleData data = new RuleData();
        ListTag list = tag.getList("on", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            Rule rule = Rule.byId(list.getString(i));
            if (rule != null) {
                data.enabled.add(rule);
            }
        }
        return data;
    }
}