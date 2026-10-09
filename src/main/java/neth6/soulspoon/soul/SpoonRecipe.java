package neth6.soulspoon.soul;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpoonRecipe extends SavedData {
    private static final String KEY = "soulspoon_recipe";

    // ID del objeto -> cantidad necesaria. Los IDs pueden ser de otros mods.
    private final Map<ResourceLocation, Integer> ingredients = new LinkedHashMap<>();

    public static SpoonRecipe get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        SpoonRecipe::createDefault,
                        SpoonRecipe::load,
                        DataFixTypes.LEVEL
                ),
                KEY
        );
    }

    private static SpoonRecipe createDefault() {
        SpoonRecipe recipe = new SpoonRecipe();
        recipe.fillDefault();
        return recipe;
    }

    private void fillDefault() {
        ingredients.clear();
        ingredients.put(ResourceLocation.withDefaultNamespace("soul_sand"), 4);
        ingredients.put(ResourceLocation.withDefaultNamespace("diamond"), 1);
        ingredients.put(ResourceLocation.withDefaultNamespace("stick"), 1);
    }

    public Map<ResourceLocation, Integer> getAll() {
        return Collections.unmodifiableMap(ingredients);
    }

    // Agrega el ingrediente o cambia su cantidad.
    public void set(ResourceLocation id, int amount) {
        ingredients.put(id, amount);
        setDirty();
    }

    public boolean remove(ResourceLocation id) {
        boolean removed = ingredients.remove(id) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void reset() {
        fillDefault();
        setDirty();
    }

    // Texto para mostrar la receta, por ejemplo "4x minecraft:soul_sand, 1x minecraft:stick".
    public String describe() {
        if (ingredients.isEmpty()) {
            return "(vacia)";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> entry : ingredients.entrySet()) {
            parts.add(entry.getValue() + "x " + entry.getKey());
        }
        return String.join(", ", parts);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, Integer> entry : ingredients.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putString("id", entry.getKey().toString());
            item.putInt("count", entry.getValue());
            list.add(item);
        }
        tag.put("ingredients", list);
        return tag;
    }

    public static SpoonRecipe load(CompoundTag tag, HolderLookup.Provider registries) {
        SpoonRecipe recipe = new SpoonRecipe();
        ListTag list = tag.getList("ingredients", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(item.getString("id"));
            if (id != null) {
                recipe.ingredients.put(id, item.getInt("count"));
            }
        }
        return recipe;
    }
}