package neth6.soulspoon.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.codex.ContentManager;
import neth6.soulspoon.codex.QuestService;
import neth6.soulspoon.codex.RecipeFilter;

/**
 * Todos los paquetes del Codex (1.21.1: CustomPacketPayload + StreamCodec).
 * Este archivo vive en "main" para que cliente y servidor compartan los mismos tipos.
 *
 * Límites de tamaño (Minecraft): C2S ~32 KiB, S2C ~1 MiB.
 * Por eso el editor envía UNA entrada por paquete y el contenido completo baja como JSON.
 */
public final class CodexPackets {
    private CodexPackets() {}

    // ------------------------------------------------------------ Cliente -> Servidor

    /** Pide al servidor que reenvíe contenido + progreso (se manda al abrir el Codex). */
    public record RequestSyncC2S() implements CustomPacketPayload {
        public static final Type<RequestSyncC2S> TYPE = new Type<>(SoulSpoon.id("request_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestSyncC2S> CODEC =
                StreamCodec.unit(new RequestSyncC2S());

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClaimQuestC2S(String questId) implements CustomPacketPayload {
        public static final Type<ClaimQuestC2S> TYPE = new Type<>(SoulSpoon.id("claim_quest"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClaimQuestC2S> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(64), ClaimQuestC2S::questId,
                        ClaimQuestC2S::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** section: "quest" | "lore" | "mob" | "recipe". json: UNA sola entrada. */
    public record EditorSaveC2S(String section, String json, boolean delete) implements CustomPacketPayload {
        public static final Type<EditorSaveC2S> TYPE = new Type<>(SoulSpoon.id("editor_save"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EditorSaveC2S> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(16), EditorSaveC2S::section,
                        ByteBufCodecs.stringUtf8(12_000), EditorSaveC2S::json,
                        ByteBufCodecs.BOOL, EditorSaveC2S::delete,
                        EditorSaveC2S::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ------------------------------------------------------------ Servidor -> Cliente

    public record SyncContentS2C(String json) implements CustomPacketPayload {
        public static final Type<SyncContentS2C> TYPE = new Type<>(SoulSpoon.id("sync_content"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncContentS2C> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(900_000), SyncContentS2C::json,
                        SyncContentS2C::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SyncProgressS2C(String json) implements CustomPacketPayload {
        public static final Type<SyncProgressS2C> TYPE = new Type<>(SoulSpoon.id("sync_progress"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncProgressS2C> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(200_000), SyncProgressS2C::json,
                        SyncProgressS2C::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Respuesta del servidor a un guardado del editor. */
    public record EditorResultS2C(boolean ok, String message) implements CustomPacketPayload {
        public static final Type<EditorResultS2C> TYPE = new Type<>(SoulSpoon.id("editor_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EditorResultS2C> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, EditorResultS2C::ok,
                        ByteBufCodecs.stringUtf8(200), EditorResultS2C::message,
                        EditorResultS2C::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ------------------------------------------------------------ Registro

    public static void initialize() {
        // 1) Registrar los TIPOS (en ambos lados) ANTES de registrar receivers.
        PayloadTypeRegistry.playC2S().register(RequestSyncC2S.TYPE, RequestSyncC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(ClaimQuestC2S.TYPE, ClaimQuestC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(EditorSaveC2S.TYPE, EditorSaveC2S.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncContentS2C.TYPE, SyncContentS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncProgressS2C.TYPE, SyncProgressS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(EditorResultS2C.TYPE, EditorResultS2C.CODEC);

        // 2) Receivers del servidor. En 1.21.1 ya corren en el hilo principal del servidor.
        ServerPlayNetworking.registerGlobalReceiver(RequestSyncC2S.TYPE, (payload, ctx) -> {
            sendContent(ctx.player());
            QuestService.syncProgress(ctx.player());
        });

        ServerPlayNetworking.registerGlobalReceiver(ClaimQuestC2S.TYPE, (payload, ctx) ->
                QuestService.claim(ctx.player(), payload.questId()));

        ServerPlayNetworking.registerGlobalReceiver(EditorSaveC2S.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            // El botón "Editor" oculto en el cliente es solo cosmético: el permiso se valida AQUÍ.
            if (!player.hasPermissions(2)) {
                SoulSpoon.LOGGER.warn("{} intentó usar el editor sin permisos", player.getName().getString());
                ServerPlayNetworking.send(player, new EditorResultS2C(false, "Sin permisos"));
                return;
            }
            String error = ContentManager.apply(payload.section(), payload.json(), payload.delete());
            if (error != null) {
                ServerPlayNetworking.send(player, new EditorResultS2C(false, error));
                return;
            }
            ServerPlayNetworking.send(player, new EditorResultS2C(true, payload.delete() ? "Borrado" : "Guardado"));
            if (payload.section().equals("recipe")) {
                RecipeFilter.apply(player.server, true);   // aplica el (des)ocultado al instante
            }
            ContentManager.broadcast(player.server);
        });
    }

    // ------------------------------------------------------------ Helpers de envío

    public static void sendContent(ServerPlayer player) {
        ServerPlayNetworking.send(player, new SyncContentS2C(ContentManager.toJson()));
    }

    public static void sendProgress(ServerPlayer player, String json) {
        ServerPlayNetworking.send(player, new SyncProgressS2C(json));
    }
}
