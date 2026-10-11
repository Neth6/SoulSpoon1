package neth6.soulspoon.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import neth6.soulspoon.rule.Rule;
import neth6.soulspoon.rule.RuleData;
import neth6.soulspoon.util.Cmd;

import java.util.EnumMap;
import java.util.Map;

// "Momentos Reviil": eventos que el admin lanza con /dedsafio evento <nombre>.
public class EventManager {
    public enum WorldEvent {
        BUNKER("bunker", "[24] Esparce a todos los jugadores (spreadplayers)", 0),
        CREEPERS("creepers", "[29] Lluvia de creepers electricos (60 s)", 1200),
        LUNA("luna", "[38] Maldicion de la luna: mata a quien mire la luna (3 min)", 3600),
        METEORITOS("meteoritos", "[39] Meteoritos, mata a los del Nether y lo cierra", 1200),
        GAS_ALTO("gasalto", "[11] Gas toxico sobre la capa 250 (3 min)", 3600),
        GAS_PROFUNDO("gasprofundo", "[12] Gas toxico bajo la capa -1 (3 min)", 3600);

        private final String id;
        private final String description;
        private final int durationTicks;

        WorldEvent(String id, String description, int durationTicks) {
            this.id = id;
            this.description = description;
            this.durationTicks = durationTicks;
        }

        public String id() {
            return id;
        }

        public String description() {
            return description;
        }

        public static WorldEvent byId(String id) {
            for (WorldEvent event : values()) {
                if (event.id.equalsIgnoreCase(id)) {
                    return event;
                }
            }
            return null;
        }
    }

    private static final Map<WorldEvent, Integer> ACTIVE = new EnumMap<>(WorldEvent.class);

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(EventManager::onServerTick);
    }

    public static boolean isActive(WorldEvent event) {
        return ACTIVE.containsKey(event);
    }

    public static void start(MinecraftServer server, WorldEvent event) {
        broadcast(server, "Momento Reviil: " + event.id().toUpperCase());
        switch (event) {
            case BUNKER -> {
                BlockPos spawn = server.overworld().getSharedSpawnPos();
                Cmd.run(server.overworld(), Vec3.atCenterOf(spawn),
                        "spreadplayers " + spawn.getX() + " " + spawn.getZ() + " 0 1000 false @a");
                return;
            }
            case METEORITOS -> {
                RuleData.get(server).set(Rule.NETHER_CERRADO, true);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (player.serverLevel().dimension() == Level.NETHER
                            && !player.isCreative() && !player.isSpectator()) {
                        player.hurt(player.serverLevel().damageSources().genericKill(),
                                Float.MAX_VALUE);
                    }
                }
            }
            default -> {
            }
        }
        ACTIVE.put(event, event.durationTicks);
    }

    public static void stop(MinecraftServer server, WorldEvent event) {
        if (ACTIVE.remove(event) != null) {
            broadcast(server, "Termino el evento: " + event.id());
        }
    }

    public static void stopAll(MinecraftServer server) {
        for (WorldEvent event : ACTIVE.keySet().toArray(new WorldEvent[0])) {
            stop(server, event);
        }
    }

    private static void broadcast(MinecraftServer server, String text) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(text), false);
    }

    private static void onServerTick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        int tick = server.getTickCount();
        for (WorldEvent event : ACTIVE.keySet().toArray(new WorldEvent[0])) {
            switch (event) {
                case CREEPERS -> creeperRain(server, tick);
                case LUNA -> moonCurse(server, tick);
                case METEORITOS -> meteors(server, tick);
                default -> {
                }
            }
            int left = ACTIVE.get(event) - 1;
            if (left <= 0) {
                stop(server, event);
            } else {
                ACTIVE.put(event, left);
            }
        }
    }

    // [29] Cada medio segundo cae un creeper electrico cerca de cada jugador.
    private static void creeperRain(MinecraftServer server, int tick) {
        if (tick % 10 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();
            if (level.dimension() != Level.OVERWORLD || player.isSpectator()) {
                continue;
            }
            if (level.getEntitiesOfClass(Creeper.class,
                    player.getBoundingBox().inflate(24)).size() >= 8) {
                continue;
            }
            RandomSource random = level.random;
            double x = player.getX() + random.nextInt(17) - 8;
            double z = player.getZ() + random.nextInt(17) - 8;
            double y = player.getY() + 12;
            Cmd.run(level, new Vec3(x, y, z),
                    "summon minecraft:creeper " + x + " " + y + " " + z + " {powered:1b}");
        }
    }

    // [38] Quien mira la luna (de noche) muere.
    private static void moonCurse(MinecraftServer server, int tick) {
        if (tick % 10 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();
            if (level.dimension() != Level.OVERWORLD
                    || player.isCreative() || player.isSpectator()) {
                continue;
            }
            // Direccion de la luna: opuesta al sol (el sol esta arriba cuando el angulo es 0).
            double angle = level.getTimeOfDay(1.0f) * 2.0 * Math.PI;
            Vec3 moon = new Vec3(Math.sin(angle), -Math.cos(angle), 0.0);
            if (moon.y < 0.05) {
                continue;
            }
            if (player.getLookAngle().dot(moon) > 0.96) {
                player.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            }
        }
    }

    // [39] Cada segundo cae un meteorito (bola de fuego) cerca de cada jugador.
    private static void meteors(MinecraftServer server, int tick) {
        if (tick % 20 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();
            if (player.isSpectator()) {
                continue;
            }
            RandomSource random = level.random;
            double x = player.getX() + random.nextInt(41) - 20;
            double z = player.getZ() + random.nextInt(41) - 20;
            double y = Math.min(player.getY() + 40, level.getMaxBuildHeight() - 2);
            Cmd.run(level, new Vec3(x, y, z),
                    "summon minecraft:fireball " + x + " " + y + " " + z
                            + " {ExplosionPower:3,Motion:[0.0,-1.5,0.0],power:[0.0,-0.1,0.0]}");
        }
    }
}
