package neth6.soulspoon.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import neth6.soulspoon.codex.CodexContent;
import neth6.soulspoon.network.CodexPackets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Copia local (solo lectura) de lo que el servidor nos ha enviado. */
public final class ClientCodexCache {
    private ClientCodexCache() {}

    private static final Gson GSON = new Gson();

    public static CodexContent content = new CodexContent();
    public static Map<String, Integer> progress = new HashMap<>();
    public static Set<String> claimed = new HashSet<>();
    public static Set<String> discoveredMobs = new HashSet<>();
    /** Segundos que faltaban para el reinicio diario cuando llegó el paquete. */
    public static long resetIn = 0;
    public static long resetReceivedAt = 0;

    /** Último mensaje del servidor tras guardar en el editor. */
    public static String editorMessage = "";
    public static boolean editorOk = true;

    public static void setContent(String json) {
        try {
            CodexContent parsed = GSON.fromJson(json, CodexContent.class);
            if (parsed != null) content = parsed;
        } catch (JsonParseException ignored) {
            // contenido corrupto: conservamos el anterior
        }
    }

    public static void setProgress(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            Map<String, Integer> p = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("progress").entrySet()) {
                p.put(e.getKey(), e.getValue().getAsInt());
            }
            progress = p;
            if (root.has("resetIn")) {
                resetIn = root.get("resetIn").getAsLong();
                resetReceivedAt = System.currentTimeMillis();
            }
            claimed = toSet(root.getAsJsonArray("claimed"));
            discoveredMobs = toSet(root.getAsJsonArray("mobs"));
        } catch (JsonParseException | IllegalStateException | NullPointerException ignored) {
        }
    }

    private static Set<String> toSet(JsonArray array) {
        Set<String> set = new HashSet<>();
        for (JsonElement e : array) set.add(e.getAsString());
        return set;
    }

    /** "5h 12m" hasta el reinicio diario, descontando lo ya transcurrido. */
    public static String resetText() {
        long left = Math.max(0, resetIn - (System.currentTimeMillis() - resetReceivedAt) / 1000);
        return (left / 3600) + "h " + ((left % 3600) / 60) + "m";
    }

    public static boolean isUnlocked(CodexContent.Quest q) {
        for (String req : q.requires) {
            if (!claimed.contains(req)) return false;
        }
        return true;
    }

    public static void requestSync() {
        if (ClientPlayNetworking.canSend(CodexPackets.RequestSyncC2S.TYPE)) {
            ClientPlayNetworking.send(new CodexPackets.RequestSyncC2S());
        }
    }
}
