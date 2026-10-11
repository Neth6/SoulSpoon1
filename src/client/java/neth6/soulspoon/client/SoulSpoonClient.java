package neth6.soulspoon.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import neth6.soulspoon.client.entity.ModEntityRenderers;
import neth6.soulspoon.network.CodexPackets;
import org.lwjgl.glfw.GLFW;

public class SoulSpoonClient implements ClientModInitializer {
    private static KeyMapping openCodex;

    @Override
    public void onInitializeClient() {
        ModEntityRenderers.register();
        ModEntitiesClient.initialize();

        openCodex = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.soulspoon.codex",
                GLFW.GLFW_KEY_K,
                "category.soulspoon"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openCodex.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new CodexScreen());
                }
            }
        });

        // Receivers S2C: en 1.21.1 ya se ejecutan en el hilo del cliente.
        ClientPlayNetworking.registerGlobalReceiver(CodexPackets.SyncContentS2C.TYPE,
                (payload, ctx) -> ClientCodexCache.setContent(payload.json()));

        ClientPlayNetworking.registerGlobalReceiver(CodexPackets.SyncProgressS2C.TYPE,
                (payload, ctx) -> ClientCodexCache.setProgress(payload.json()));

        ClientPlayNetworking.registerGlobalReceiver(CodexPackets.EditorResultS2C.TYPE,
                (payload, ctx) -> {
                    ClientCodexCache.editorOk = payload.ok();
                    ClientCodexCache.editorMessage = payload.message();
                });
    }
}