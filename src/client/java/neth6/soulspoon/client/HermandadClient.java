package neth6.soulspoon.client;

import com.mojang.blaze3d.platform.InputConstants;
import neth6.soulspoon.boveda.ModBoveda;
import neth6.soulspoon.client.boveda.BovedaRenderer;
import neth6.soulspoon.client.boveda.BovedaScreen;
import neth6.soulspoon.hermandad.*;
import neth6.soulspoon.item.ManuscritoHermandadItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Llamar a HermandadClient.initialize() desde SoulSpoonClient.onInitializeClient(). */
public final class HermandadClient {
    public static final KeyMapping TECLA_HERMANDAD = KeyBindingHelper.registerKeyBinding(
        new KeyMapping("key.soulspoon.hermandad", InputConstants.Type.KEYSYM, 72, "category.soulspoon"));
    private static boolean pedida;

    private HermandadClient() {}

    public static void initialize() {
        BlockEntityRenderers.register(ModBoveda.ENTIDAD, BovedaRenderer::new);
        MenuScreens.register(ModBoveda.MENU, BovedaScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(ModBoveda.EstadoPayload.TYPE, (p, c) -> BovedaScreen.recibir(p));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TECLA_HERMANDAD.consumeClick()) {
                if (client.screen == null) {
                    pedida = true;
                    ClientPlayNetworking.send(new HermandadAccionPayload(0, ""));
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(HermandadChatPayload.TYPE, (p, c) -> HermandadesCliente.actualizarChat(p));
        ClientPlayNetworking.registerGlobalReceiver(HermandadUbicacionesPayload.TYPE, (p, c) -> HermandadesCliente.actualizarUbicaciones(p));
        ClientPlayNetworking.registerGlobalReceiver(HermandadTablonPayload.TYPE, (p, c) -> {
            HermandadesCliente.actualizarTablon(p);
            if (c.client().screen instanceof HermandadScreen s) s.refrescarTablon();
        });
        ClientPlayNetworking.registerGlobalReceiver(HermandadEstandartesPayload.TYPE, (p, c) -> {
            HermandadesCliente.actualizarEstandartes(p);
            if (c.client().screen instanceof HermandadScreen s) s.refrescarEstandartes();
        });
        ClientPlayNetworking.registerGlobalReceiver(HermandadesJugadoresPayload.TYPE, (p, c) -> HermandadesCliente.actualizar(p));
        ClientPlayNetworking.registerGlobalReceiver(HermandadInfoPayload.TYPE, (p, c) -> {
            Minecraft mc = c.client();
            if (mc.screen instanceof HermandadScreen s) s.actualizar(p);
            else if (mc.screen == null && pedida) mc.setScreen(new HermandadScreen(p));
            pedida = false;
        });

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((tipo, dibujante, registro, contexto) -> {
            if (dibujante instanceof PlayerRenderer jugador) {
                registro.register(new CapaEstandarteLayer(jugador, contexto.getModelSet()));
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HermandadesCliente.limpiar());

        for (Item m : ModHermandad.MANUSCRITOS) {
            ItemProperties.register(m, ResourceLocation.fromNamespaceAndPath("soulspoon", "sellado"),
                (stack, level, e, seed) -> ManuscritoDatos.sellado(stack) ? 1.0F : 0.0F);
        }
        ManuscritoHermandadItem.abrirPantalla = mano -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new ManuscritoScreen(mano, mc.player.getItemInHand(mano)));
        };
    }
}
