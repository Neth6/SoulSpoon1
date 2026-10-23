package neth6.soulspoon;

import neth6.soulspoon.candados.CandadoCodigoPayload;
import neth6.soulspoon.candados.CandadoPantallaPayload;
import neth6.soulspoon.candados.Candados;
import neth6.soulspoon.candados.CandadosListaPayload;
import neth6.soulspoon.candados.ModCandados;
import neth6.soulspoon.intercambio.ModIntercambio;
import neth6.soulspoon.marcas.BloqueoMinimapa;
import neth6.soulspoon.marcas.Marcas;
import neth6.soulspoon.marcas.MarcasPayload;
import neth6.soulspoon.marcas.MinimapaPayload;
import neth6.soulspoon.subastas.ModVitrina;
import neth6.soulspoon.subastas.Subastas;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.CreativeModeTabs;

/** Subastas/Vitrina, Candados, Marcas, Minimapa e Intercambio. Llamar ModSocial.initialize() antes de ImportedContent. */
public final class ModSocial {
    private ModSocial() {}

    public static void initialize() {
        ModVitrina.initialize();
        ModIntercambio.initialize();
        ModCandados.initialize();
        Subastas.registrarRed();

        PayloadTypeRegistry.playS2C().register(MarcasPayload.TYPE, MarcasPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MinimapaPayload.TYPE, MinimapaPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CandadoPantallaPayload.TYPE, CandadoPantallaPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CandadosListaPayload.TYPE, CandadosListaPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CandadoCodigoPayload.TYPE, CandadoCodigoPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CandadoCodigoPayload.TYPE,
            (payload, ctx) -> Candados.codigoEscrito(ctx.player(), payload));

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            Subastas.registrar(dispatcher);
            BloqueoMinimapa.registrarComando(dispatcher);
            Marcas.registrarComandos(dispatcher);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Subastas.entregarCobros(handler.getPlayer());
            Marcas.sincronizar(handler.getPlayer());
            BloqueoMinimapa.alEntrar(handler.getPlayer());
        });
        ServerTickEvents.END_SERVER_TICK.register(BloqueoMinimapa::tick);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> {
            e.accept(ModVitrina.VITRINA_ITEM);
            e.accept(ModIntercambio.PUESTO_ITEM);
        });
    }
}
