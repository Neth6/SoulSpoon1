package neth6.soulspoon.wheel;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public class EntryMenu extends ChestMenu {
    private static final int PER_PAGE = 45;
    private static final int PREV_SLOT = 45;
    private static final int RANDOM_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final SimpleContainer container;
    private final MinecraftServer server;
    private final WheelColor color;
    private final List<WheelRegistry.Entry> entries;
    private int page = 0;

    private EntryMenu(int containerId, Inventory inventory, SimpleContainer container,
                      MinecraftServer server, WheelColor color) {
        super(MenuType.GENERIC_9x6, containerId, inventory, container, 6);
        this.container = container;
        this.server = server;
        this.color = color;
        this.entries = WheelRegistry.byColor(color);
        refresh();
    }

    public static void open(ServerPlayer player, WheelColor color) {
        SimpleContainer container = new SimpleContainer(54);
        MinecraftServer server = player.server;
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) ->
                        new EntryMenu(containerId, inventory, container, server, color),
                Component.literal(color.colorName() + ": " + color.title())));
    }

    private void refresh() {
        for (int i = 0; i < 54; i++) {
            container.setItem(i, i >= PER_PAGE ? filler() : ItemStack.EMPTY);
        }

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < entries.size(); i++) {
            container.setItem(i, entryStack(entries.get(start + i)));
        }

        if (entries.isEmpty()) {
            ItemStack none = new ItemStack(Items.BARRIER);
            none.set(DataComponents.CUSTOM_NAME,
                    Component.literal("Aun no hay cambios registrados para este color"));
            container.setItem(22, none);
            return;
        }
        if (page > 0) {
            container.setItem(PREV_SLOT, button(Items.ARROW, "Pagina anterior"));
        }
        if (start + PER_PAGE < entries.size()) {
            container.setItem(NEXT_SLOT, button(Items.ARROW, "Pagina siguiente"));
        }
        container.setItem(RANDOM_SLOT, button(Items.ENDER_EYE, "Elegir uno al azar"));
    }

    private static ItemStack filler() {
        ItemStack pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        return pane;
    }

    private static ItemStack button(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private ItemStack entryStack(WheelRegistry.Entry entry) {
        ItemStack stack = new ItemStack(color.icon());
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(entry.name()).withStyle(color.format()));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal(entry.description()),
                Component.literal("Clic para aplicar"))));
        return stack;
    }

    // Se llama con cada clic dentro del menu.
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Anulamos cualquier movimiento de items.
        sendAllDataToRemote();

        if (clickType != ClickType.PICKUP || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (slotId >= 0 && slotId < PER_PAGE) {
            int index = page * PER_PAGE + slotId;
            if (index < entries.size()) {
                apply(serverPlayer, entries.get(index));
            }
        } else if (slotId == PREV_SLOT && page > 0) {
            page--;
            refresh();
        } else if (slotId == NEXT_SLOT && (page + 1) * PER_PAGE < entries.size()) {
            page++;
            refresh();
        } else if (slotId == RANDOM_SLOT && !entries.isEmpty()) {
            apply(serverPlayer, entries.get(serverPlayer.getRandom().nextInt(entries.size())));
        }
    }

    private void apply(ServerPlayer player, WheelRegistry.Entry entry) {
        entry.action().accept(server);
        player.closeContainer();
    }
}