package neth6.soulspoon.sound;

import com.mojang.brigadier.arguments.StringArgumentType;
import neth6.soulspoon.SoulSpoon;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Sonidos propios (assets/soulspoon/sounds.json + sounds/*.ogg). */
public final class ModSounds {
    private ModSounds() {}

    public static final SoundEvent RULETA = register("ruleta");
    public static final SoundEvent NOTIFY_MESSAGE = register("notify_message");

    /** Duracion de ruleta.ogg (14,93 s) en ticks: la animacion de la ruleta dura lo mismo. */
    public static final int RULETA_TICKS = 299;

    private static SoundEvent register(String name) {
        ResourceLocation id = SoulSpoon.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void initialize() {
        SoulSpoon.LOGGER.info("Sonidos registrados: {} y {}", RULETA.getLocation(), NOTIFY_MESSAGE.getLocation());
        // Prueba: /sonido ruleta | /sonido notify  (lo oye solo quien lo escribe)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("sonido")
                        .then(Commands.argument("cual", StringArgumentType.word())
                                .suggests((ctx, b) -> b.suggest("ruleta").suggest("notify").buildFuture())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String which = StringArgumentType.getString(ctx, "cual");
                                    SoundEvent ev = which.equals("ruleta") ? RULETA : NOTIFY_MESSAGE;
                                    p.playNotifySound(ev, SoundSource.MASTER, 1.0f, 1.0f);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Sonido enviado: " + ev.getLocation()), false);
                                    return 1;
                                }))));
    }

    /** Lo escuchan todos los jugadores, sin importar donde esten. */
    public static void playAll(MinecraftServer server, SoundEvent sound) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.playNotifySound(sound, SoundSource.MASTER, 1.0f, 1.0f);
        }
    }
}
