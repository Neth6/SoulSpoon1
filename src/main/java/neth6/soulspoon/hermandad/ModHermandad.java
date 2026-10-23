package neth6.soulspoon.hermandad;

import java.util.List;
import neth6.soulspoon.item.ManuscritoHermandadItem;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Llamar a ModHermandad.initialize() (y antes ModBoveda.initialize()) desde SoulSpoon.onInitialize, antes de ImportedContent. */
public final class ModHermandad {
    public static final Item MANUSCRITO_HERMANDAD = Registry.register(BuiltInRegistries.ITEM,
        ResourceLocation.fromNamespaceAndPath("soulspoon", "manuscrito_hermandad"),
        new ManuscritoHermandadItem(new Item.Properties().stacksTo(1), 3, false));
    public static final Item MANUSCRITO_HERMANDAD_ADMIN = Registry.register(BuiltInRegistries.ITEM,
        ResourceLocation.fromNamespaceAndPath("soulspoon", "manuscrito_hermandad_admin"),
        new ManuscritoHermandadItem(new Item.Properties().stacksTo(1), 1, true));
    public static final List<Item> MANUSCRITOS = List.of(MANUSCRITO_HERMANDAD, MANUSCRITO_HERMANDAD_ADMIN);

    private ModHermandad() {}

    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(HermandadInfoPayload.TYPE, HermandadInfoPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HermandadUbicacionesPayload.TYPE, HermandadUbicacionesPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HermandadTablonPayload.TYPE, HermandadTablonPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HermandadEstandartesPayload.TYPE, HermandadEstandartesPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HermandadChatPayload.TYPE, HermandadChatPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HermandadesJugadoresPayload.TYPE, HermandadesJugadoresPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ManuscritoAccionPayload.TYPE, ManuscritoAccionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HermandadAccionPayload.TYPE, HermandadAccionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ManuscritoAccionPayload.TYPE,
            (payload, ctx) -> Hermandades.alAccion(ctx.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(HermandadAccionPayload.TYPE,
            (payload, ctx) -> Hermandades.alAccionInterfaz(ctx.player(), payload));

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> Hermandades.registrarComandos(dispatcher));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Hermandades.sincronizarJugador(handler.getPlayer());
            Hermandades.enviarUbicaciones(handler.getPlayer());
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> {
            e.accept(MANUSCRITO_HERMANDAD);
            e.accept(MANUSCRITO_HERMANDAD_ADMIN);
        });
    }
}
