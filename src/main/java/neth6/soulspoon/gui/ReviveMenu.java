package neth6.soulspoon.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import neth6.soulspoon.item.ModItems;
import neth6.soulspoon.soul.DeadPlayers;
import neth6.soulspoon.soul.ReviveManager;
import neth6.soulspoon.soul.SoulManager;

import java.util.ArrayList;
import java.util.List;

public class ReviveMenu extends ChestMenu {
    private static final int SLOTS = 27;

    // Nombres de los muertos, en el mismo orden que las cabezas del menu.
    private final List<String> names;

    private ReviveMenu(int containerId, Inventory inventory,
                       Container container, List<String> names) {
        super(MenuType.GENERIC_9x3, containerId, inventory, container, 3);
        this.names = names;
    }

    // Abre el menu para el jugador (solo en el servidor).
    public static void open(ServerPlayer player) {
        DeadPlayers dead = DeadPlayers.get(player.server);
        List<String> all = new ArrayList<>(dead.getAll().values());

        if (all.isEmpty()) {
            player.displayClientMessage(Component.literal("Nadie esta muerto"), true);
            return;
        }

        List<String> shown = new ArrayList<>(all.subList(0, Math.min(all.size(), SLOTS)));

        SimpleContainer container = new SimpleContainer(SLOTS);
        for (int i = 0; i < shown.size(); i++) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.CUSTOM_NAME, Component.literal(shown.get(i)));
            container.setItem(i, head);
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) ->
                        new ReviveMenu(containerId, inventory, container, shown),
                Component.literal("Elige a quien revivir")));
    }

    // Se llama con cada clic dentro del menu.
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Anulamos cualquier movimiento de items: resincronizamos al cliente.
        sendAllDataToRemote();

        boolean simpleClick = clickType == ClickType.PICKUP;
        boolean onHead = slotId >= 0 && slotId < names.size();

        if (simpleClick && onHead && player instanceof ServerPlayer serverPlayer) {
            reviveByIndex(serverPlayer, slotId);
        }
    }

    private void reviveByIndex(ServerPlayer player, int index) {
        String name = names.get(index);

        // La cuchara debe estar en una de las dos manos.
        ItemStack spoon = player.getMainHandItem();
        if (!spoon.is(ModItems.SOUL_SPOON)) {
            spoon = player.getOffhandItem();
        }
        if (!spoon.is(ModItems.SOUL_SPOON)) {
            player.displayClientMessage(Component.literal(
                    "Necesitas la Cuchara de Almas en la mano"), true);
            player.closeContainer();
            return;
        }

        ReviveManager.Result result = ReviveManager.revive(player, name);

        switch (result) {
            case SUCCESS -> {
                if (!player.isCreative()) {
                    spoon.shrink(1);
                }
                player.displayClientMessage(Component.literal(
                        name + " fue revivido. Te quedan "
                                + SoulManager.getSouls(player) + " almas"), false);
            }
            case NO_SOULS -> player.displayClientMessage(Component.literal(
                    "No te quedan almas para revivir"), true);
            case NOT_DEAD -> player.displayClientMessage(Component.literal(
                    name + " ya no esta muerto"), true);
        }

        player.closeContainer();
    }
}