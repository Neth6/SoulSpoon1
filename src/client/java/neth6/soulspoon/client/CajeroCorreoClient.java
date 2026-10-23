package neth6.soulspoon.client;

import neth6.soulspoon.banco.CajeroBlock;
import neth6.soulspoon.banco.ModCajero;
import neth6.soulspoon.client.cajero.CajeroIngresarScreen;
import neth6.soulspoon.client.cajero.CajeroScreen;
import neth6.soulspoon.client.correo.BuzonScreen;
import neth6.soulspoon.client.correo.CorreoCliente;
import neth6.soulspoon.client.correo.CorreoScreen;
import neth6.soulspoon.correo.Correo;
import neth6.soulspoon.correo.MensajeroBlock;
import neth6.soulspoon.correo.ModCorreo;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;

/** Llamar a CajeroCorreoClient.initialize() desde SoulSpoonClient.onInitializeClient(). */
public final class CajeroCorreoClient {
    private CajeroCorreoClient() {}

    public static void initialize() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModCajero.CAJERO, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModCorreo.MENSAJERO, RenderType.cutout());

        CajeroBlock.abrirPantalla = pos -> Minecraft.getInstance().setScreen(new CajeroScreen(pos));
        MensajeroBlock.abrirPantalla = pos -> Minecraft.getInstance().setScreen(new BuzonScreen());

        MenuScreens.register(Correo.MENU, CorreoScreen::new);
        MenuScreens.register(ModCajero.MENU, CajeroIngresarScreen::new);

        CorreoCliente.registrar();
        // Sobre con la cantidad de cartas sin leer, debajo del panel de deditas.
        HudRenderCallback.EVENT.register((g, tick) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;
            CorreoCliente.dibujarSobre(g, mc, mc.player.getActiveEffects().isEmpty() ? 36 : 86);
        });
    }
}
