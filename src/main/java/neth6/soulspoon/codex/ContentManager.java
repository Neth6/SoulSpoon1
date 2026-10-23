package neth6.soulspoon.codex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.network.CodexPackets;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Carga, valida, guarda y difunde el contenido del Codex. Solo vive en el servidor. */
public final class ContentManager {
    private ContentManager() {}

    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT = new Gson();
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_.:/-]{1,64}");
    private static final Set<String> OBJECTIVES = Set.of("KILL", "OBTAIN", "DIMENSION");

    private static CodexContent content = new CodexContent();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("soulspoon").resolve("codex.json");
    }

    public static CodexContent get() {
        return content;
    }

    public static String toJson() {
        return COMPACT.toJson(content);
    }

    public static Optional<CodexContent.Quest> quest(String id) {
        return content.quests.stream().filter(q -> q.id.equals(id)).findFirst();
    }

    // ------------------------------------------------------------------ disco

    public static void load() {
        Path path = file();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                CodexContent loaded = COMPACT.fromJson(reader, CodexContent.class);
                content = loaded != null ? loaded : new CodexContent();
                if (migrateRewards()) save();
                return;
            } catch (IOException | JsonParseException e) {
                SoulSpoon.LOGGER.error("No se pudo leer {}; se usa contenido de ejemplo", path, e);
            }
        }
        content = defaults();
        save();
    }

    /**
     * Pasa las misiones de ejemplo y las de la ruleta de recompensas en items a deditas.
     * Devuelve true si cambio algo (entonces se guarda codex.json).
     */
    private static boolean migrateRewards() {
        boolean changed = false;
        for (CodexContent.Quest q : content.quests) {
            if (q.rewardDeditas > 0) continue;
            long deditas = 0;
            if (q.id.equals("primera_sangre") && "minecraft:iron_ingot".equals(q.rewardItem)) deditas = 10;
            else if (q.id.equals("cazador_de_cuero") && "minecraft:diamond".equals(q.rewardItem)) deditas = 20;
            else if (q.id.equals("diaria_madera") && "minecraft:golden_apple".equals(q.rewardItem)) deditas = 15;
            else if (q.id.startsWith("ruleta_")) deditas = RuletaCodex.rewardFor(q.id);
            if (deditas > 0) {
                q.rewardDeditas = deditas;
                q.rewardItem = "";
                q.rewardCount = 1;
                changed = true;
            }
        }
        return changed;
    }

    public static void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                PRETTY.toJson(content, writer);
            }
        } catch (IOException e) {
            SoulSpoon.LOGGER.error("No se pudo guardar {}", path, e);
        }
    }

    private static CodexContent defaults() {
        CodexContent c = new CodexContent();

        CodexContent.Quest q = new CodexContent.Quest();
        q.id = "primera_sangre";
        q.category = "Combate";
        q.chapter = "Capítulo I";
        q.title = "Primera sangre";
        q.description = "Derrota a 5 zombis.";
        q.objectiveType = "KILL";
        q.target = "minecraft:zombie";
        q.amount = 5;
        q.rewardDeditas = 10;
        c.quests.add(q);

        CodexContent.Quest q2 = new CodexContent.Quest();
        q2.id = "cazador_de_cuero";
        q2.category = "Combate";
        q2.chapter = "Capítulo I";
        q2.title = "Cazador de esqueletos";
        q2.description = "Derrota a 10 esqueletos. Requiere 'Primera sangre'.";
        q2.requires.add("primera_sangre");
        q2.objectiveType = "KILL";
        q2.target = "minecraft:skeleton";
        q2.amount = 10;
        q2.rewardDeditas = 20;
        c.quests.add(q2);

        CodexContent.Quest d = new CodexContent.Quest();
        d.id = "diaria_madera";
        d.category = "Recolección";
        d.chapter = "Diarias";
        d.title = "Leñador del día";
        d.description = "Consigue 32 troncos de roble. Se reinicia cada día.";
        d.objectiveType = "OBTAIN";
        d.target = "minecraft:oak_log";
        d.amount = 32;
        d.rewardDeditas = 15;
        d.daily = true;
        c.quests.add(d);

        CodexContent.Lore l = new CodexContent.Lore();
        l.id = "bienvenida";
        l.title = "Bienvenida";
        l.text = "Este mundo guarda secretos. Muere y alguien deberá traerte de vuelta con la Cuchara de Almas.";
        c.lore.add(l);

        CodexContent.Mob m = new CodexContent.Mob();
        m.id = "minecraft:zombie";
        m.name = "Zombi";
        m.danger = "Bajo";
        m.description = "Muerto viviente que ataca en grupo, sobre todo de noche.";
        m.tips = "Mantén distancia y usa un escudo.";
        m.loot.add("minecraft:rotten_flesh");
        m.discover = "always";
        c.mobs.add(m);
        return c;
    }

    // ------------------------------------------------------------------ edición

    /**
     * Aplica una edición del Editor. Devuelve null si todo fue bien, o el mensaje de error.
     * El cliente NO es de fiar: todo se valida aquí.
     */
    public static String apply(String section, String json, boolean delete) {
        JsonObject obj;
        try {
            obj = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            return "JSON inválido";
        }

        switch (section) {
            case "quest" -> {
                CodexContent.Quest q;
                try {
                    q = COMPACT.fromJson(obj, CodexContent.Quest.class);
                } catch (JsonParseException e) {
                    return "Misión inválida";
                }
                String err = checkId(q.id);
                if (err != null) return err;
                if (delete) {
                    removeById(content.quests, q.id);
                    // Quita la misión borrada de los requisitos de las demás.
                    content.quests.forEach(o -> o.requires.remove(q.id));
                } else {
                    err = sanitizeQuest(q);
                    if (err != null) return err;
                    upsert(content.quests, q);
                }
            }
            case "lore" -> {
                CodexContent.Lore l;
                try {
                    l = COMPACT.fromJson(obj, CodexContent.Lore.class);
                } catch (JsonParseException e) {
                    return "Entrada de lore inválida";
                }
                String err = checkId(l.id);
                if (err != null) return err;
                if (delete) {
                    removeById(content.lore, l.id);
                } else {
                    l.title = cut(l.title, 80);
                    l.text = cut(l.text, 4000);
                    l.reveal = (l.reveal == null || l.reveal.isBlank()) ? "always" : cut(l.reveal, 80);
                    if (!l.reveal.equals("always") && !l.reveal.startsWith("quest:")) {
                        return "Revelado debe ser 'always' o 'quest:<id>'";
                    }
                    upsert(content.lore, l);
                }
            }
            case "mob" -> {
                CodexContent.Mob m;
                try {
                    m = COMPACT.fromJson(obj, CodexContent.Mob.class);
                } catch (JsonParseException e) {
                    return "Mob inválido";
                }
                String err = checkId(m.id);
                if (err != null) return err;
                if (delete) {
                    removeById(content.mobs, m.id);
                } else {
                    m.entity = cut(m.entity, 64);
                    m.customName = cut(m.customName, 60);
                    String base = m.entity.isBlank() ? m.id : m.entity;
                    ResourceLocation rl = ResourceLocation.tryParse(base);
                    if (rl == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
                        return "Entidad desconocida: " + base;
                    }
                    m.name = cut(m.name, 60);
                    m.danger = cut(m.danger, 30);
                    m.description = cut(m.description, 1500);
                    m.tips = cut(m.tips, 1500);
                    if (m.loot == null) m.loot = new java.util.ArrayList<>();
                    m.loot = m.loot.stream().limit(16).map(s -> cut(s, 64)).toList();
                    m.discover = "always".equals(m.discover) ? "always" : "kill";
                    upsert(content.mobs, m);
                }
            }
            case "recipe" -> {
                String id = obj.has("id") ? obj.get("id").getAsString() : "";
                String err = checkId(id);
                if (err != null) return err;
                if (delete) {
                    content.hiddenRecipes.remove(id);
                } else if (!content.hiddenRecipes.contains(id)) {
                    content.hiddenRecipes.add(id);
                }
            }
            default -> {
                return "Sección desconocida";
            }
        }
        save();
        return null;
    }

    private static String sanitizeQuest(CodexContent.Quest q) {
        q.category = cut(q.category, 40);
        q.chapter = cut(q.chapter, 40);
        q.title = cut(q.title, 80);
        q.description = cut(q.description, 1500);
        if (q.requires == null) q.requires = new java.util.ArrayList<>();
        q.requires = new java.util.ArrayList<>(q.requires.stream().limit(10).toList());
        if (q.requires.contains(q.id)) return "Una misión no puede requerirse a sí misma";
        q.objectiveType = q.objectiveType == null ? "" : q.objectiveType.toUpperCase();
        if (!OBJECTIVES.contains(q.objectiveType)) return "Tipo debe ser KILL, OBTAIN o DIMENSION";
        q.amount = Math.max(1, Math.min(q.amount, 100_000));
        ResourceLocation target = ResourceLocation.tryParse(q.target == null ? "" : q.target);
        if (target == null) return "Objetivo inválido";
        boolean known = switch (q.objectiveType) {
            case "KILL" -> BuiltInRegistries.ENTITY_TYPE.containsKey(target);
            case "OBTAIN" -> BuiltInRegistries.ITEM.containsKey(target);
            default -> true; // las dimensiones pueden venir de datapacks
        };
        if (!known) return "Objetivo desconocido: " + q.target;
        if (q.objectiveType.equals("DIMENSION")) q.amount = 1;
        if (q.rewardItem != null && !q.rewardItem.isBlank()) {
            ResourceLocation r = ResourceLocation.tryParse(q.rewardItem);
            if (r == null || !BuiltInRegistries.ITEM.containsKey(r)) return "Ítem de recompensa desconocido";
        } else {
            q.rewardItem = "";
        }
        q.rewardCount = Math.max(1, Math.min(q.rewardCount, 64));
        q.rewardDeditas = Math.max(0L, Math.min(q.rewardDeditas, 100_000_000L));
        return null;
    }

    private static String checkId(String id) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            return "ID inválido (a-z 0-9 _ . : / -, máx. 64)";
        }
        return null;
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static <T> void upsert(List<T> list, T item) {
        String id = idOf(item);
        for (int i = 0; i < list.size(); i++) {
            if (idOf(list.get(i)).equals(id)) {
                list.set(i, item);
                return;
            }
        }
        list.add(item);
    }

    private static <T> void removeById(List<T> list, String id) {
        list.removeIf(o -> idOf(o).equals(id));
    }

    private static String idOf(Object o) {
        if (o instanceof CodexContent.Quest q) return q.id;
        if (o instanceof CodexContent.Lore l) return l.id;
        if (o instanceof CodexContent.Mob m) return m.id;
        return "";
    }

    // ------------------------------------------------------------------ red

    /** Reenvía contenido y progreso a todos los jugadores conectados. */
    public static void broadcast(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            CodexPackets.sendContent(p);
            QuestService.syncProgress(p);
        }
    }
}
