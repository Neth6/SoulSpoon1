package neth6.soulspoon.casino;

import java.util.List;
import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.economy.Dedita;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Paquetes del casino: el servidor manda la tabla; un admin la devuelve editada. */
public final class CasinoNet {
    private CasinoNet() {}

    /** Servidor -> cliente: abre la pantalla de la tabla. */
    public record TablaS2C(boolean editable, long precio, int fichas, List<CasinoPool.Entry> entradas)
            implements CustomPacketPayload {
        public static final Type<TablaS2C> TYPE = new Type<>(SoulSpoon.id("casino_tabla"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TablaS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, TablaS2C::editable,
                ByteBufCodecs.VAR_LONG, TablaS2C::precio,
                ByteBufCodecs.VAR_INT, TablaS2C::fichas,
                CasinoPool.LIST_CODEC, TablaS2C::entradas,
                TablaS2C::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Cliente -> servidor (solo admins): guarda la tabla. Lista vacia = restablecer la de fabrica. */
    public record GuardarC2S(long precio, List<CasinoPool.Entry> entradas) implements CustomPacketPayload {
        public static final Type<GuardarC2S> TYPE = new Type<>(SoulSpoon.id("casino_guardar"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GuardarC2S> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, GuardarC2S::precio,
                CasinoPool.LIST_CODEC, GuardarC2S::entradas,
                GuardarC2S::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(TablaS2C.TYPE, TablaS2C.CODEC);
        PayloadTypeRegistry.playC2S().register(GuardarC2S.TYPE, GuardarC2S.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(GuardarC2S.TYPE, (payload, ctx) -> {
            ServerPlayer jugador = ctx.player();
            if (!jugador.createCommandSourceStack().hasPermission(2)) return;   // solo admins
            CasinoPremios.Modo m = CasinoPremios.modo(jugador.server);
            m.precio = Math.max(0L, Math.min(1_000_000L, payload.precio()));
            m.pool = payload.entradas().isEmpty() ? null : CasinoPool.limpiar(payload.entradas());
            m.setDirty();
            jugador.sendSystemMessage(Component.literal("Casino: tabla guardada."));
            abrir(jugador);
        });
    }

    public static int fichas(ServerPlayer jugador) {
        var ficha = BuiltInRegistries.ITEM.getOptional(SoulSpoon.id("dedita_casino")).orElse(null);
        if (ficha == null) return 0;
        int n = 0;
        for (int i = 0; i < jugador.getInventory().getContainerSize(); i++) {
            ItemStack s = jugador.getInventory().getItem(i);
            if (s.is(ficha)) n += s.getCount();
        }
        return n;
    }

    /** Abre la tabla: editable si es admin, solo lectura si no. */
    public static void abrir(ServerPlayer jugador) {
        CasinoPremios.Modo m = CasinoPremios.modo(jugador.server);
        boolean admin = jugador.createCommandSourceStack().hasPermission(2);
        ServerPlayNetworking.send(jugador, new TablaS2C(admin, m.precio, fichas(jugador), m.tabla()));
    }
}
