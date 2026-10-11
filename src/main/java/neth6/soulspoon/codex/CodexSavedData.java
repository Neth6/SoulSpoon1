package neth6.soulspoon.codex;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Persistencia del Codex en el guardado del mundo.
 * - completedMissions: misiones RECLAMADAS por jugador.
 * - discoveredMobs:    mobs descubiertos por jugador.
 * - counters:          progreso de objetivos KILL / DIMENSION (clave = id de misión).
 * (El progreso OBTAIN no se guarda: se calcula del inventario al momento.)
 */
public class CodexSavedData extends SavedData {
    private static final String KEY = "soulspoon_codex_progress";

    private final Map<UUID, Set<String>> completedMissions = new HashMap<>();
    private final Map<UUID, Set<String>> discoveredMobs = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> counters = new HashMap<>();
    private final Map<UUID, Long> lastDay = new HashMap<>();

    public static CodexSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        CodexSavedData::new,
                        CodexSavedData::load,
                        DataFixTypes.LEVEL
                ),
                KEY
        );
    }

    // ---- misiones reclamadas

    public boolean isMissionCompleted(UUID player, String missionId) {
        return completedMissions.getOrDefault(player, Collections.emptySet()).contains(missionId);
    }

    public boolean completeMission(UUID player, String missionId) {
        boolean changed = completedMissions
                .computeIfAbsent(player, ignored -> new HashSet<>())
                .add(missionId);
        if (changed) setDirty();
        return changed;
    }

    /** Para el reinicio de misiones diarias. */
    public void uncompleteMission(UUID player, String missionId) {
        Set<String> set = completedMissions.get(player);
        if (set != null && set.remove(missionId)) setDirty();
    }

    /** Último día (epoch day) en que se reiniciaron las diarias de este jugador; -1 si nunca. */
    public long getLastDay(UUID player) {
        return lastDay.getOrDefault(player, -1L);
    }

    public void setLastDay(UUID player, long day) {
        lastDay.put(player, day);
        setDirty();
    }

    public Set<String> completedMissions(UUID player) {
        return Collections.unmodifiableSet(completedMissions.getOrDefault(player, Collections.emptySet()));
    }

    // ---- mobs descubiertos

    public boolean hasDiscoveredMob(UUID player, String mobId) {
        return discoveredMobs.getOrDefault(player, Collections.emptySet()).contains(mobId);
    }

    public boolean discoverMob(UUID player, String mobId) {
        boolean changed = discoveredMobs
                .computeIfAbsent(player, ignored -> new HashSet<>())
                .add(mobId);
        if (changed) setDirty();
        return changed;
    }

    public Set<String> discoveredMobs(UUID player) {
        return Collections.unmodifiableSet(discoveredMobs.getOrDefault(player, Collections.emptySet()));
    }

    // ---- contadores

    public int getCounter(UUID player, String key) {
        return counters.getOrDefault(player, Collections.emptyMap()).getOrDefault(key, 0);
    }

    public void setCounter(UUID player, String key, int value) {
        counters.computeIfAbsent(player, ignored -> new HashMap<>()).put(key, value);
        setDirty();
    }

    // ---- NBT

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("completed_missions", saveMap(completedMissions));
        tag.put("discovered_mobs", saveMap(discoveredMobs));

        ListTag counterList = new ListTag();
        counters.forEach((uuid, values) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", uuid);
            CompoundTag vals = new CompoundTag();
            values.forEach(vals::putInt);
            entry.put("values", vals);
            counterList.add(entry);
        });
        tag.put("counters", counterList);

        ListTag dayList = new ListTag();
        lastDay.forEach((uuid, day) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", uuid);
            entry.putLong("day", day);
            dayList.add(entry);
        });
        tag.put("last_day", dayList);
        return tag;
    }

    private static ListTag saveMap(Map<UUID, Set<String>> source) {
        ListTag list = new ListTag();
        source.forEach((uuid, values) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", uuid);
            ListTag ids = new ListTag();
            for (String value : values) ids.add(StringTag.valueOf(value));
            entry.put("ids", ids);
            list.add(entry);
        });
        return list;
    }

    private static void loadMap(ListTag list, Map<UUID, Set<String>> target) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID uuid = entry.getUUID("player");
            ListTag ids = entry.getList("ids", Tag.TAG_STRING);
            Set<String> values = new HashSet<>();
            for (int j = 0; j < ids.size(); j++) values.add(ids.getString(j));
            target.put(uuid, values);
        }
    }

    public static CodexSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CodexSavedData data = new CodexSavedData();
        loadMap(tag.getList("completed_missions", Tag.TAG_COMPOUND), data.completedMissions);
        loadMap(tag.getList("discovered_mobs", Tag.TAG_COMPOUND), data.discoveredMobs);

        ListTag counterList = tag.getList("counters", Tag.TAG_COMPOUND);
        for (int i = 0; i < counterList.size(); i++) {
            CompoundTag entry = counterList.getCompound(i);
            CompoundTag vals = entry.getCompound("values");
            Map<String, Integer> map = new HashMap<>();
            for (String key : vals.getAllKeys()) map.put(key, vals.getInt(key));
            data.counters.put(entry.getUUID("player"), map);
        }

        ListTag dayList = tag.getList("last_day", Tag.TAG_COMPOUND);
        for (int i = 0; i < dayList.size(); i++) {
            CompoundTag entry = dayList.getCompound(i);
            data.lastDay.put(entry.getUUID("player"), entry.getLong("day"));
        }
        return data;
    }
}
