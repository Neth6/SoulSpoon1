package neth6.soulspoon.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import neth6.soulspoon.soul.SpoonRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecipeMenu extends ChestMenu {
    private static final int ROWS = 3;
    private static final int TOTAL_SLOTS = ROWS * 9;
    // Las dos primeras filas son para ingredientes; la ultima, para botones.
    private static final int INGREDIENT_SLOTS = 18;
    private static final int HELP_SLOT = 18;
    private static final int RESET_SLOT = 26;
    private static final int MAX_AMOUNT = 64;

    private final SimpleContainer container;
    private final MinecraftServer server;
    private final boolean canEdit;

    // IDs de los ingredientes, en el mismo orden que las casillas del menu.
    private List<ResourceLocation> shown = new ArrayList<>();

    private RecipeMenu(int containerId, Inventory inventory,
                       SimpleContainer container, MinecraftServer server, boolean canEdit) {
        super(MenuType.GENERIC_9x3, containerId, inventory, container, ROWS);
        this.container = container;
        this.server = server;
        this.canEdit = canEdit;
        refresh();
    }

    // Abre el menu. Si canEdit es false, el jugador solo puede mirar.
    public static void open(ServerPlayer player, boolean canEdit) {
        SimpleContainer container = new SimpleContainer(TOTAL_SLOTS);
        MinecraftServer server = player.server;

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) ->
                        new RecipeMenu(containerId, inventory, container, server, canEdit),
                Component.literal(canEdit
                        ? "Receta de la Cuchara de Almas"
                        : "Receta (solo lectura)")));
    }

    // Vuelve a dibujar el contenido del menu segun la receta guardada.
    private void refresh() {
        SpoonRecipe recipe = SpoonRecipe.get(server);
        shown = new ArrayList<>(recipe.getAll().keySet());

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            container.setItem(i, filler());
        }
        for (int i = 0; i < INGREDIENT_SLOTS; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        for (int i = 0; i < shown.size() && i < INGREDIENT_SLOTS; i++) {
            ResourceLocation id = shown.get(i);
            container.setItem(i, ingredientStack(id, recipe.getAll().get(id)));
        }

        container.setItem(HELP_SLOT, helpStack());
        container.setItem(RESET_SLOT, canEdit ? resetStack() : filler());
    }

    private ItemStack ingredientStack(ResourceLocation id, int amount) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        List<Component> lore = new ArrayList<>();
        ItemStack stack;

        if (item.isPresent()) {
            stack = new ItemStack(item.get(), amount);
        } else {
            // El objeto es de un mod que ya no esta instalado.
            stack = new ItemStack(Items.BARRIER);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Objeto no encontrado"));
            lore.add(Component.literal(id.toString()));
        }

        lore.add(Component.literal("Cantidad: " + amount));
        if (canEdit) {
            lore.add(Component.literal("Clic izquierdo: +1"));
            lore.add(Component.literal("Clic derecho: -1"));
            lore.add(Component.literal("Shift + clic: quitar"));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private ItemStack filler() {
        ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        return pane;
    }

    private ItemStack helpStack() {
        ItemStack book = new ItemStack(Items.BOOK);
        book.set(DataComponents.CUSTOM_NAME, Component.literal("Como usar"));

        List<Component> lore = new ArrayList<>();
        if (canEdit) {
            lore.add(Component.literal("Clic en un objeto de TU inventario: agregarlo"));
            lore.add(Component.literal("Clic en un ingrediente: cambiar la cantidad"));
        } else {
            lore.add(Component.literal("Solo los operadores pueden editar la receta"));
        }
        book.set(DataComponents.LORE, new ItemLore(lore));
        return book;
    }

    private ItemStack resetStack() {
        ItemStack block = new ItemStack(Items.REDSTONE_BLOCK);
        block.set(DataComponents.CUSTOM_NAME, Component.literal("Reiniciar receta"));
        block.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Vuelve a la receta por defecto"))));
        return block;
    }

    // Se llama con cada clic dentro del menu.
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        boolean usefulClick = clickType == ClickType.PICKUP
                || clickType == ClickType.QUICK_MOVE;

        if (canEdit && usefulClick) {
            handleClick(slotId, button, clickType);
        }

        // Anulamos cualquier movimiento de items y actualizamos al cliente.
        sendAllDataToRemote();
    }

    private void handleClick(int slotId, int button, ClickType clickType) {
        SpoonRecipe recipe = SpoonRecipe.get(server);

        if (slotId >= TOTAL_SLOTS) {
            // Clic en el inventario del jugador: agregar ese objeto.
            ItemStack clicked = slots.get(slotId).getItem();
            if (clicked.isEmpty()) {
                return;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(clicked.getItem());
            boolean isNew = !recipe.getAll().containsKey(id);
            if (isNew && recipe.getAll().size() >= INGREDIENT_SLOTS) {
                return;
            }
            int current = recipe.getAll().getOrDefault(id, 0);
            recipe.set(id, Math.min(MAX_AMOUNT, current + 1));

        } else if (slotId == RESET_SLOT) {
            recipe.reset();

        } else if (slotId >= 0 && slotId < INGREDIENT_SLOTS && slotId < shown.size()) {
            ResourceLocation id = shown.get(slotId);
            int current = recipe.getAll().getOrDefault(id, 0);
            int next = current;

            if (clickType == ClickType.QUICK_MOVE) {
                next = 0;
            } else if (button == 0) {
                next = Math.min(MAX_AMOUNT, current + 1);
            } else if (button == 1) {
                next = current - 1;
            }

            if (next <= 0) {
                recipe.remove(id);
            } else {
                recipe.set(id, next);
            }

        } else {
            return;
        }

        refresh();
    }
}