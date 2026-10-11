package neth6.soulspoon.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import neth6.soulspoon.network.CodexPackets;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Lógica autoritativa de misiones: progreso, requisitos y reclamo de recompensas. */
public final class QuestService {
    private QuestService() {}

    public static void initialize() {
        // Sincroniza contenido y progreso al entrar.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            CodexPackets.sendContent(player);
            syncProgress(player);
        });

        // Kills: cuenta para misiones KILL y descubre el mob en el Bestiario.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(source.getEntity() instanceof ServerPlayer killer)) return;
            String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            CodexSavedData data = CodexSavedData.get(killer.server);
            UUID uuid = killer.getUUID();

            ensureDailyReset(killer, data);
            // Descubre las entradas del Bestiario que coincidan con la entidad (y su nombre, si lo piden).
            String killedName = entity.hasCustomName() ? entity.getCustomName().getString() : "";
            for (CodexContent.Mob m : ContentManager.get().mobs) {
                String base = (m.entity == null || m.entity.isBlank()) ? m.id : m.entity;
                boolean nameOk = m.customName == null || m.customName.isBlank()
                        || m.customName.equals(killedName);
                if (base.equals(entityId) && nameOk) data.discoverMob(uuid, m.id);
            }
            for (CodexContent.Quest q : ContentManager.get().quests) {
                if (q.objectiveType.equals("KILL") && q.target.equals(entityId)
                        && !data.isMissionCompleted(uuid, q.id)) {
                    int current = data.getCounter(uuid, q.id);
                    if (current < q.amount) data.setCounter(uuid, q.id, current + 1);
                }
            }
            syncProgress(killer);
        });

        // Dimensiones.
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            String dim = destination.dimension().location().toString();
            CodexSavedData data = CodexSavedData.get(player.server);
            ensureDailyReset(player, data);
            for (CodexContent.Quest q : ContentManager.get().quests) {
                if (q.objectiveType.equals("DIMENSION") && q.target.equals(dim)) {
                    data.setCounter(player.getUUID(), q.id, 1);
                }
            }
            syncProgress(player);
        });

        // Reinicio diario también para quien lleva horas conectado (revisión cada minuto).
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 1200 != 0) return;
            CodexSavedData data = CodexSavedData.get(server);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (ensureDailyReset(p, data)) syncProgress(p);
            }
        });
    }

    // ------------------------------------------------------------------ diarias

    /**
     * Si cambió el día (hora local del servidor) desde el último reinicio de este jugador,
     * borra progreso y "reclamada" de las misiones diarias. Devuelve true si hubo reinicio.
     */
    public static boolean ensureDailyReset(ServerPlayer player, CodexSavedData data) {
        UUID uuid = player.getUUID();
        long today = LocalDate.now().toEpochDay();
        if (data.getLastDay(uuid) == today) return false;

        data.setLastDay(uuid, today);
        for (CodexContent.Quest q : ContentManager.get().quests) {
            if (q.daily) {
                data.uncompleteMission(uuid, q.id);
                data.setCounter(uuid, q.id, 0);
            }
        }
        return true;
    }

    /** Segundos hasta la próxima medianoche (hora local del servidor). */
    public static long secondsUntilReset() {
        LocalDateTime now = LocalDateTime.now();
        return Duration.between(now, LocalDate.now().plusDays(1).atStartOfDay()).getSeconds();
    }

    // ------------------------------------------------------------------ progreso

    public static int progress(ServerPlayer player, CodexContent.Quest q, CodexSavedData data) {
        int value;
        if (q.objectiveType.equals("OBTAIN")) {
            value = countItems(player, q.target);
        } else {
            value = data.getCounter(player.getUUID(), q.id);
        }
        return Math.min(value, q.amount);
    }

    private static int countItems(ServerPlayer player, String itemId) {
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(itemId)) {
                total += s.getCount();
            }
        }
        return total;
    }

    private static void consumeItems(ServerPlayer player, String itemId, int amount) {
        Inventory inv = player.getInventory();
        int left = amount;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(itemId)) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
    }

    public static boolean requirementsMet(CodexContent.Quest q, UUID player, CodexSavedData data) {
        for (String req : q.requires) {
            if (!data.isMissionCompleted(player, req)) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ reclamar

    public static void claim(ServerPlayer player, String questId) {
        Optional<CodexContent.Quest> opt = ContentManager.quest(questId);
        if (opt.isEmpty()) return;
        CodexContent.Quest q = opt.get();

        CodexSavedData data = CodexSavedData.get(player.server);
        UUID uuid = player.getUUID();

        ensureDailyReset(player, data);
        if (data.isMissionCompleted(uuid, q.id)) return;
        if (!requirementsMet(q, uuid, data)) {
            player.displayClientMessage(Component.literal("Misión bloqueada."), true);
            return;
        }
        if (progress(player, q, data) < q.amount) {
            player.displayClientMessage(Component.literal("Aún no has completado el objetivo."), true);
            return;
        }

        if (q.objectiveType.equals("OBTAIN")) consumeItems(player, q.target, q.amount);
        data.completeMission(uuid, q.id);

        if (!q.rewardItem.isBlank()) {
            ResourceLocation rl = ResourceLocation.tryParse(q.rewardItem);
            Optional<Item> item = rl == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(rl);
            item.ifPresent(i -> {
                ItemStack stack = new ItemStack(i, q.rewardCount);
                player.getInventory().add(stack);
                if (!stack.isEmpty()) player.drop(stack, false); // inventario lleno: cae al suelo
            });
        }
        player.displayClientMessage(Component.literal("¡Recompensa reclamada: " + q.title + "!"), true);
        syncProgress(player);
    }

    // ------------------------------------------------------------------ sync

    public static void syncProgress(ServerPlayer player) {
        CodexSavedData data = CodexSavedData.get(player.server);
        UUID uuid = player.getUUID();
        ensureDailyReset(player, data);

        JsonObject root = new JsonObject();
        root.addProperty("resetIn", secondsUntilReset());
        JsonObject prog = new JsonObject();
        for (CodexContent.Quest q : ContentManager.get().quests) {
            prog.addProperty(q.id, progress(player, q, data));
        }
        root.add("progress", prog);

        JsonArray claimed = new JsonArray();
        data.completedMissions(uuid).forEach(claimed::add);
        root.add("claimed", claimed);

        JsonArray mobs = new JsonArray();
        data.discoveredMobs(uuid).forEach(mobs::add);
        root.add("mobs", mobs);

        CodexPackets.sendProgress(player, root.toString());
    }
}
