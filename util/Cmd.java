package neth6.soulspoon.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class Cmd {
    private Cmd() {
    }

    // Ejecuta un comando de vanilla como el servidor (permiso 4) y sin mostrar salida.
    public static void run(ServerLevel level, Vec3 pos, String command) {
        MinecraftServer server = level.getServer();
        CommandSourceStack source = server.createCommandSourceStack()
                .withLevel(level)
                .withPosition(pos)
                .withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }

    public static void run(MinecraftServer server, String command) {
        run(server.overworld(), Vec3.ZERO, command);
    }

    // data merge entity <uuid> <nbt>
    public static void merge(net.minecraft.world.entity.Entity entity, String nbt) {
        if (entity.level() instanceof ServerLevel level) {
            run(level, entity.position(), "data merge entity " + entity.getUUID() + " " + nbt);
        }
    }
}
