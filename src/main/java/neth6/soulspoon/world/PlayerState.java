package neth6.soulspoon.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Contadores por jugador (en segundos): insomnia, infeccion y inmunidad.
public class PlayerState extends SavedData {
    private static final String KEY = "soulspoon_player_state";

    public static final int INSOMNIA_HIGH = 1200;      // 20 min despierto = insomnia alta
    public static final int INFECTION_START = 7200;    // 2 h hasta que la infeccion llega a 0
    public static final int PILL_IMMUNITY = 4 * 3600;  // la Pildora da 4 horas

    private final Map<UUID, Integer> insomnia = new HashMap<>();
    private final Map<UUID, Integer> infection = new HashMap<>();
    private final Map<UUID, Integer> immune = new HashMap<>();

    public static PlayerState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PlayerState::new, PlayerState::load, DataFixTypes.LEVEL),
                KEY);
    }

    public int insomnia(UUID id) {
        return insomnia.getOrDefault(id, 0);
    }

    public void setInsomnia(UUID id, int value) {
        insomnia.put(id, value);
        setDirty();
    }

    public int infection(UUID id) {
        return infection.getOrDefault(id, INFECTION_START);
    }

    public void setInfection(UUID id, int value) {
        infection.put(id, value);
        setDirty();
    }

    public int immune(UUID id) {
        return immune.getOrDefault(id, 0);
    }

    public void setImmune(UUID id, int value) {
        immune.put(id, value);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("insomnia", toTag(insomnia));
        tag.put("infection", toTag(infection));
        tag.put("immune", toTag(immune));
        return tag;
    }

    public static PlayerState load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerState state = new PlayerState();
        fromTag(tag.getCompound("insomnia"), state.insomnia);
        fromTag(tag.getCompound("infection"), state.infection);
        fromTag(tag.getCompound("immune"), state.immune);
        return state;
    }

    private static CompoundTag toTag(Map<UUID, Integer> map) {
        CompoundTag tag = new CompoundTag();
        map.forEach((id, value) -> tag.putInt(id.toString(), value));
        return tag;
    }

    private static void fromTag(CompoundTag tag, Map<UUID, Integer> map) {
        for (String key : tag.getAllKeys()) {
            map.put(UUID.fromString(key), tag.getInt(key));
        }
    }
}
