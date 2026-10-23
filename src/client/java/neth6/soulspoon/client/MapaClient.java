package neth6.soulspoon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

/** Llamar a MapaClient.initialize() desde SoulSpoonClient.onInitializeClient(). */
public final class MapaClient {
    public static final KeyMapping TECLA_MAPA = KeyBindingHelper.registerKeyBinding(
        new KeyMapping("key.soulspoon.mapa", InputConstants.Type.KEYSYM, 77, "category.soulspoon"));

    private MapaClient() {}

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TECLA_MAPA.consumeClick()) {
                if (client.screen == null && client.level != null) client.setScreen(new MapaScreen());
            }
        });
    }
}
