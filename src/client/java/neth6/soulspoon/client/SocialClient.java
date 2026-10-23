package neth6.soulspoon.client;

import java.util.Set;
import neth6.soulspoon.candados.CandadoPantallaPayload;
import neth6.soulspoon.candados.Candados;
import neth6.soulspoon.candados.CandadosListaPayload;
import neth6.soulspoon.client.subastas.SubastasScreen;
import neth6.soulspoon.client.subastas.VitrinaRenderer;
import neth6.soulspoon.marcas.MarcasPayload;
import neth6.soulspoon.marcas.MinimapaPayload;
import neth6.soulspoon.subastas.ModVitrina;
import neth6.soulspoon.subastas.Subastas;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/** Llamar a SocialClient.initialize() desde SoulSpoonClient.onInitializeClient(). */
public final class SocialClient {
    private SocialClient() {}

    public static void initialize() {
        MenuScreens.register(Subastas.MENU, SubastasScreen::new);
        BlockEntityRenderers.register(ModVitrina.ENTIDAD, VitrinaRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(CandadosListaPayload.TYPE,
            (p, c) -> Candados.cerradosCliente = Set.copyOf(p.posiciones()));
        ClientPlayNetworking.registerGlobalReceiver(CandadoPantallaPayload.TYPE,
            (p, c) -> c.client().execute(() -> c.client().setScreen(new CandadoScreen(p.pos(), p.cerrar(), p.aviso()))));
        ClientPlayNetworking.registerGlobalReceiver(MinimapaPayload.TYPE, (p, c) -> XaeroBloqueo.adminsLoUsan = p.adminsLoUsan());
        ClientPlayNetworking.registerGlobalReceiver(MarcasPayload.TYPE, (p, c) -> MarcasCliente.actualizar(p));
        WorldRenderEvents.LAST.register(MarcasCliente::dibujar);
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> {
            MarcasCliente.limpiar();
            Candados.cerradosCliente = Set.of();
        });
    }
}
