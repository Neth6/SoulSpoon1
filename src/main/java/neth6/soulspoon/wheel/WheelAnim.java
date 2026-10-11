package neth6.soulspoon.wheel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import neth6.soulspoon.SoulSpoon;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Animaciones: se muestran los fotogramas (y al final el ultimo fotograma) en el centro de la
 * pantalla de todos los jugadores. Los fotogramas son "letras" de una fuente propia
 * (assets/soulspoon/font/wheel.json), asi no hace falta codigo en el cliente.
 * Los archivos los genera tools/gif_to_wheel.py.
 *
 * Nombres: los colores ("rojo", "morado"...) y los extras ("death", "reviil", "nutria"...).
 */
public final class WheelAnim {
    private WheelAnim() {}

    private static final ResourceLocation FONT = SoulSpoon.id("wheel");
    private static final Map<String, Config> CONFIG = new HashMap<>();
    private static final List<Playing> PLAYING = new ArrayList<>();

    private record Config(int slot, int frames, int delay, int loops, boolean hasFinal, int finalTicks) {}

    public static void initialize() {
        load();
        ServerTickEvents.END_SERVER_TICK.register(server -> PLAYING.removeIf(Playing::tick));
        // Animacion de muerte: la ven todos cuando muere un jugador.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                play(player.server, "death", () -> { });
            }
        });
    }

    private static void load() {
        try (InputStream in = WheelAnim.class.getResourceAsStream("/assets/soulspoon/wheel_anim.json")) {
            if (in == null) return;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String name : root.keySet()) {
                JsonObject o = root.getAsJsonObject(name);
                CONFIG.put(name, new Config(
                        o.get("slot").getAsInt(),
                        o.get("frames").getAsInt(),
                        Math.max(1, o.get("delay").getAsInt()),
                        Math.max(1, o.get("loops").getAsInt()),
                        o.get("final").getAsBoolean(),
                        o.get("final_ticks").getAsInt()));
            }
        } catch (Exception e) {
            SoulSpoon.LOGGER.warn("No se pudo leer wheel_anim.json: {}", e.toString());
        }
    }

    private static String glyph(Config c, int n) {
        return String.valueOf((char) (0xE000 + c.slot() * 64 + n));
    }

    private static String key(WheelColor color) {
        return color.name().toLowerCase();
    }

    public static int duration(WheelColor color) { return duration(key(color)); }

    public static int duration(String name) {
        Config c = CONFIG.get(name);
        if (c == null) return 0;
        return c.frames() * c.delay() * c.loops() + (c.hasFinal() ? c.finalTicks() : 0);
    }

    public static void play(MinecraftServer server, WheelColor color, Runnable after) {
        play(server, key(color), after);
    }

    /** Reproduce la animacion "name" y al terminar ejecuta "after". Sin animacion: ejecuta "after" ya. */
    public static void play(MinecraftServer server, String name, Runnable after) {
        Config c = CONFIG.get(name);
        if (c == null || (c.frames() == 0 && !c.hasFinal())) {
            after.run();
            return;
        }
        PLAYING.add(new Playing(server, c, after));
    }

    private static void showAll(MinecraftServer server, String glyph, int stay) {
        Component text = Component.literal(glyph).setStyle(Style.EMPTY.withFont(FONT));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(0, stay, 0));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
            p.connection.send(new ClientboundSetTitleTextPacket(text));
        }
    }

    private static final class Playing {
        private final MinecraftServer server;
        private final Config cfg;
        private final Runnable after;
        private int t = 0;

        Playing(MinecraftServer server, Config cfg, Runnable after) {
            this.server = server;
            this.cfg = cfg;
            this.after = after;
        }

        boolean tick() {
            int animTicks = cfg.frames() * cfg.delay() * cfg.loops();
            if (t < animTicks) {
                if (t % cfg.delay() == 0) {
                    int frame = (t / cfg.delay()) % cfg.frames();
                    showAll(server, glyph(cfg, frame), cfg.delay() + 2);
                }
            } else if (cfg.hasFinal() && t < animTicks + cfg.finalTicks()) {
                if (t == animTicks) showAll(server, glyph(cfg, 63), cfg.finalTicks());
            } else {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.connection.send(new ClientboundClearTitlesPacket(true));
                }
                after.run();
                return true;
            }
            t++;
            return false;
        }
    }
}
