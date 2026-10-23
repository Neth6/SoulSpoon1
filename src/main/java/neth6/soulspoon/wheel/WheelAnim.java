package neth6.soulspoon.wheel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import neth6.soulspoon.SoulSpoon;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
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

    // base = codigo de la primera letra de la animacion; finalGlyph = letra del fotograma final
    private record Config(int base, int finalGlyph, int frames, int delay, int loops, boolean hasFinal, int finalTicks) {}

    public static void initialize() {
        load();
        ServerTickEvents.END_SERVER_TICK.register(server -> PLAYING.removeIf(Playing::tick));
        // La animacion de muerte ya NO se lanza aqui: la maneja soul/DeathSequence (animacion -> espectador -> expulsion).

        // Prueba: /animacion <nombre>   (rojo, morado, verde, amarillo, cian, azul, naranja, muerte, nutria, reviil...)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("animacion")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("nombre", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    CONFIG.keySet().forEach(b::suggest);
                                    return b.buildFuture();
                                })
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "nombre");
                                    Config c = CONFIG.get(name);
                                    if (c == null) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "No hay animacion '" + name + "'. Disponibles: " + CONFIG.keySet()));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Animacion " + name + ": " + c.frames() + " fotogramas, letra inicial U+"
                                                    + Integer.toHexString(c.base()).toUpperCase()), false);
                                    play(ctx.getSource().getServer(), name, () -> { });
                                    return 1;
                                }))));
    }

    public static boolean has(String name) {
        Config c = CONFIG.get(name);
        return c != null && (c.frames() > 0 || c.hasFinal());
    }

    private static void load() {
        try (InputStream in = WheelAnim.class.getResourceAsStream("/assets/soulspoon/wheel_anim.json")) {
            if (in == null) return;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String name : root.keySet()) {
                JsonObject o = root.getAsJsonObject(name);
                // "base" lo escribe el gif_to_wheel nuevo; si falta, se usa el esquema viejo (64 letras por slot).
                int slot = o.get("slot").getAsInt();
                int base = o.has("base") ? o.get("base").getAsInt() : 0xE000 + slot * 64;
                int finalGlyph = o.has("final_glyph") ? o.get("final_glyph").getAsInt() : base + 63;
                CONFIG.put(name, new Config(
                        base, finalGlyph,
                        o.get("frames").getAsInt(),
                        Math.max(1, o.get("delay").getAsInt()),
                        Math.max(1, o.get("loops").getAsInt()),
                        o.get("final").getAsBoolean(),
                        o.get("final_ticks").getAsInt()));
            }
        } catch (Exception e) {
            SoulSpoon.LOGGER.warn("No se pudo leer wheel_anim.json: {}", e.toString());
        }
        SoulSpoon.LOGGER.info("WheelAnim: {} animaciones cargadas {}", CONFIG.size(), CONFIG.keySet());
    }

    private static String glyph(Config c, int n) {
        return String.valueOf((char) (c.base() + n));
    }

    private static String finalGlyph(Config c) {
        return String.valueOf((char) c.finalGlyph());
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

    /**
     * Igual que play, pero la animacion se estira (o se acorta) para durar EXACTAMENTE "totalTicks"
     * (incluido el fotograma final). Sirve para que acabe a la vez que el audio de la ruleta.
     */
    public static void playFitted(MinecraftServer server, WheelColor color, int totalTicks, Runnable after) {
        Config c = CONFIG.get(key(color));
        if (c == null || (c.frames() == 0 && !c.hasFinal())) {
            after.run();
            return;
        }
        PLAYING.add(new Playing(server, c, after, totalTicks));
    }

    /** Reproduce la animacion "name" y al terminar ejecuta "after". Sin animacion: ejecuta "after" ya. */
    public static void play(MinecraftServer server, String name, Runnable after) {
        Config c = CONFIG.get(name);
        if (c == null || (c.frames() == 0 && !c.hasFinal())) {
            after.run();
            return;
        }
        PLAYING.add(new Playing(server, c, after, 0));
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
        private final int fit; // 0 = velocidad normal; >0 = duracion total en ticks
        private int t = 0;
        private int lastFrame = -1;

        Playing(MinecraftServer server, Config cfg, Runnable after, int fit) {
            this.server = server;
            this.cfg = cfg;
            this.after = after;
            this.fit = fit;
        }

        boolean tick() {
            int finalTicks = cfg.hasFinal() ? cfg.finalTicks() : 0;
            int animTicks = fit > 0 ? Math.max(cfg.frames(), fit - finalTicks)
                                    : cfg.frames() * cfg.delay() * cfg.loops();
            if (t < animTicks) {
                if (fit > 0) {
                    // Los fotogramas se reparten por igual en todo el tiempo disponible.
                    int frame = (int) ((long) t * cfg.frames() / animTicks);
                    if (frame != lastFrame) {
                        lastFrame = frame;
                        showAll(server, glyph(cfg, frame), animTicks / Math.max(1, cfg.frames()) + 3);
                    }
                } else if (t % cfg.delay() == 0) {
                    int frame = (t / cfg.delay()) % cfg.frames();
                    showAll(server, glyph(cfg, frame), cfg.delay() + 2);
                }
            } else if (cfg.hasFinal() && t < animTicks + cfg.finalTicks()) {
                if (t == animTicks) showAll(server, finalGlyph(cfg), cfg.finalTicks());
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
