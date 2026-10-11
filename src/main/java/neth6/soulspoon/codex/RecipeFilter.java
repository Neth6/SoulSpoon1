package neth6.soulspoon.codex;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Oculta/desactiva recetas (vanilla o de mods) según la lista "hiddenRecipes" del editor.
 *
 * Sin mixins: guardamos una copia completa de las recetas cuando el servidor arranca o se
 * recargan los datapacks, y cada vez que cambia la lista reemplazamos el contenido del
 * RecipeManager por la copia filtrada y reenviamos las recetas a los clientes.
 */
public final class RecipeFilter {
    private RecipeFilter() {}

    /** Copia SIN filtrar de todas las recetas. */
    private static List<RecipeHolder<?>> all = new ArrayList<>();

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            snapshot(server);
            apply(server, false);
        });
        // Tras /reload el manager vuelve a estar completo: nueva copia y se vuelve a filtrar.
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success) return;
            snapshot(server);
            apply(server, true);
        });
    }

    private static void snapshot(MinecraftServer server) {
        all = new ArrayList<>(server.getRecipeManager().getRecipes());
    }

    /** @param resync si true, reenvía recetas/tags a los jugadores conectados. */
    public static void apply(MinecraftServer server, boolean resync) {
        if (all.isEmpty()) snapshot(server);
        Set<String> hidden = Set.copyOf(ContentManager.get().hiddenRecipes);

        List<RecipeHolder<?>> filtered = all.stream()
                .filter(h -> !hidden.contains(h.id().toString()))
                .collect(Collectors.toList());

        RecipeManager manager = server.getRecipeManager();
        manager.replaceRecipes(filtered);

        if (resync) {
            // Reenvía ClientboundUpdateRecipesPacket + libro de recetas.
            server.getPlayerList().reloadResources();
        }
    }
}
