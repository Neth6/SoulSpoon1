package neth6.soulspoon.wheel;

import neth6.soulspoon.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import neth6.soulspoon.sound.ModSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.util.ArrayList;
import java.util.List;

public class WheelMenu extends ChestMenu {
    // Los 7 colores van en la fila del medio: casillas 10 a 16.
    private static final int FIRST_COLOR_SLOT = 10;
    private static final int SPIN_SLOT = 22;

    private final SimpleContainer container;
    private final MinecraftServer server;
    private boolean spinning = false;

    private WheelMenu(int containerId, Inventory inventory,
                      SimpleContainer container, MinecraftServer server) {
        super(MenuType.GENERIC_9x3, containerId, inventory, container, 3);
        this.container = container;
        this.server = server;
        refresh(-1);
    }

    public static void open(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(27);
        MinecraftServer server = player.server;
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) ->
                        new WheelMenu(containerId, inventory, container, server),
                Component.literal("Ruleta DEDSAFIO")));
    }

    // Llamado cada tick desde ModWheel.
    public static void tickSpins() {
        // Ya no hay giro por pasos: la animacion la maneja WheelAnim.
    }

    // Dibuja el menu. "pointer" es el color marcado por la flecha (-1 = ninguno).
    private void refresh(int pointer) {
        WheelData data = WheelData.get(server);

        for (int i = 0; i < 27; i++) {
            container.setItem(i, filler());
        }
        for (WheelColor color : WheelColor.values()) {
            container.setItem(FIRST_COLOR_SLOT + color.ordinal(),
                    colorStack(color, data.isOn(color)));
        }
        if (pointer >= 0) {
            ItemStack arrow = new ItemStack(Items.ARROW);
            arrow.set(DataComponents.CUSTOM_NAME, Component.literal("v"));
            container.setItem(FIRST_COLOR_SLOT - 9 + pointer, arrow);
        }

        ItemStack spin = new ItemStack(Items.NETHER_STAR);
        spin.set(DataComponents.CUSTOM_NAME, Component.literal("Girar la ruleta"));
        spin.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Cae en uno de los colores encendidos"))));
        container.setItem(SPIN_SLOT, spin);
    }

    private static ItemStack filler() {
        ItemStack pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        return pane;
    }

    private static ItemStack colorStack(WheelColor color, boolean on) {
        ItemStack stack = new ItemStack(on ? color.icon() : Items.GRAY_STAINED_GLASS_PANE);
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(color.colorName() + " - " + color.title())
                        .withStyle(on ? color.format() : ChatFormatting.GRAY));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal(color.description()));
        lore.add(Component.literal(on
                ? "ENCENDIDO (clic para apagar)"
                : "APAGADO (clic para encender)"));
        stack.set(DataComponents.LORE, new ItemLore(lore));

        if (on) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return stack;
    }

    // Se llama con cada clic dentro del menu.
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Anulamos cualquier movimiento de items.
        sendAllDataToRemote();

        if (spinning || clickType != ClickType.PICKUP
                || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        int colorCount = WheelColor.values().length;
        if (slotId >= FIRST_COLOR_SLOT && slotId < FIRST_COLOR_SLOT + colorCount) {
            WheelColor color = WheelColor.values()[slotId - FIRST_COLOR_SLOT];
            WheelData data = WheelData.get(server);
            data.set(color, !data.isOn(color));
            refresh(-1);
        } else if (slotId == SPIN_SLOT) {
            startSpin(serverPlayer);
        }
    }

    // Giro instantaneo: se cierra el menu y la animacion del color empieza al momento.
    private void startSpin(ServerPlayer player) {
        List<WheelColor> enabled = WheelData.get(server).enabledColors();
        if (enabled.isEmpty()) {
            player.displayClientMessage(Component.literal("Enciende al menos un color"), true);
            return;
        }
        spinning = true;
        WheelColor landed = enabled.get(player.getRandom().nextInt(enabled.size()));
        player.closeContainer();
        ModSounds.playAll(server, ModSounds.RULETA);
        WheelAnim.playFitted(server, landed, ModSounds.RULETA_TICKS, () -> {
            WheelRegistry.broadcast(server, landed,
                    landed.colorName().toUpperCase() + ": " + landed.title(),
                    landed.description());
            if (!player.hasDisconnected()) {
                EntryMenu.open(player, landed);
            }
        });
    }
}
