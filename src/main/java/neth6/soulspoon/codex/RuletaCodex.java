package neth6.soulspoon.codex;

import net.minecraft.server.MinecraftServer;
import neth6.soulspoon.wheel.WheelColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Une la ruleta con el Codex:
 *  - addMission(): cuando la ruleta anuncia una mision principal, se agrega al Codex (categoria "Ruleta").
 *  - record(): cada anuncio de la ruleta queda guardado como entrada del Codex (historial "Ruleta - ...").
 *
 * Todo se guarda en codex.json con ContentManager, asi que sobrevive reinicios y se sincroniza
 * con los jugadores igual que las demas misiones.
 */
public final class RuletaCodex {
    private RuletaCodex() {}

    public static final String CATEGORY = "Ruleta";
    private static final String HISTORY_PREFIX = "ruleta_";
    private static final int MAX_HISTORY = 60;

    // Misiones principales predefinidas (guia V2). El objetivo OBTAIN usa el item del mod
    // que se creara en las Partes 4/6; hasta entonces la mision aparece pero no se puede reclamar.
    private record Def(String id, String title, String description, String type, String target) {}

    private static final List<Def> MAIN = List.of(
            new Def("dia1", "Dia 1: Aldeano Phora",
                    "Pica y recoge un Aldeano Phora de Recursos dentro de un Cubo Phora.",
                    "OBTAIN", "soulspoon:aldeano_phora_recursos"),
            new Def("dia2", "Dia 2: Bolsa Primitiva",
                    "Crea una Bolsa Primitiva (receta en el menu con la tecla G).",
                    "OBTAIN", "soulspoon:bolsa_primitiva"),
            new Def("dia5", "Dia 5: Plataforma de Lanzamiento",
                    "Crea una Plataforma de Lanzamiento (vital para etapas avanzadas y viajes espaciales).",
                    "OBTAIN", "soulspoon:plataforma_lanzamiento"),
            new Def("dia7", "Dia 7: Nave Espacial Biplaza",
                    "Construye una Nave Espacial Biplaza y Tanques de Dilitio para despegar a Gleba (busqueda del Mazo de Hami).",
                    "OBTAIN", "soulspoon:nave_biplaza"),
            new Def("dia8", "Dia 8: Fruto de Quiu",
                    "Crea y consume un Fruto de Quiu para obtener Keep Inventory (se resetea al morir o revivir).",
                    "OBTAIN", "soulspoon:fruto_quiu"),
            new Def("dia9", "Dia 9: Bolsa Glebanoide",
                    "Crea una Bolsa Glebanoide (mayor espacio que las Bolsas Primitivas; receta en la G).",
                    "OBTAIN", "soulspoon:bolsa_glebanoide"),
            new Def("dia15", "Dia 15: Traje Interdimensional",
                    "Fabrica el Traje Interdimensional para sobrevivir a la magia toxica entre dimensiones y buscar a Reviil.",
                    "OBTAIN", "soulspoon:traje_interdimensional"));

    /** Recompensa en azules: las faciles 10-20 azules, las complicadas 2-5 verdes (100 c/u). */
    public static long rewardFor(String questId) {
        return switch (questId.replace("ruleta_", "")) {
            case "dia1" -> 10;
            case "dia2" -> 15;
            case "dia5" -> 200;
            case "dia7" -> 500;
            case "dia8" -> 300;
            case "dia9" -> 300;
            case "dia15" -> 500;
            default -> 0;
        };
    }

    /** Ids de las misiones principales (dia1, dia2, dia5, ...). */
    public static List<String> mainIds() {
        List<String> ids = new ArrayList<>();
        for (Def d : MAIN) ids.add(d.id());
        return ids;
    }

    public static String mainTitle(String id) {
        for (Def d : MAIN) if (d.id().equals(id)) return d.title();
        return id;
    }

    public static String mainDescription(String id) {
        for (Def d : MAIN) if (d.id().equals(id)) return d.description();
        return "";
    }

    /** Agrega la mision principal al Codex (si ya estaba, no hace nada). */
    public static void addMission(MinecraftServer server, String mainId) {
        Def def = null;
        for (Def d : MAIN) if (d.id().equals(mainId)) def = d;
        if (def == null) return;

        CodexContent content = ContentManager.get();
        String questId = "ruleta_" + def.id();
        for (CodexContent.Quest q : content.quests) {
            if (questId.equals(q.id)) return;
        }

        CodexContent.Quest q = new CodexContent.Quest();
        q.id = questId;
        q.category = CATEGORY;
        q.chapter = "Misiones principales";
        q.title = def.title();
        q.description = def.description();
        q.requires = new ArrayList<>();
        q.objectiveType = def.type();
        q.target = def.target();
        q.amount = 1;
        q.rewardItem = "";
        q.rewardCount = 1;
        q.rewardDeditas = rewardFor(questId);
        q.daily = false;
        content.quests.add(q);

        ContentManager.save();
        ContentManager.broadcast(server);
    }

    /** Guarda el anuncio de la ruleta como entrada del Codex (historial, solo las ultimas 60). */
    public static void record(MinecraftServer server, WheelColor color, String title, String text) {
        CodexContent content = ContentManager.get();

        CodexContent.Lore lore = new CodexContent.Lore();
        lore.id = HISTORY_PREFIX + color.name().toLowerCase() + "_" + Long.toString(System.currentTimeMillis(), 36);
        lore.title = cut("Ruleta " + color.colorName() + ": " + title, 60);
        lore.text = cut(text, 400);
        lore.reveal = "always";
        content.lore.add(lore);

        // Solo se guardan las ultimas MAX_HISTORY entradas de ruleta.
        List<CodexContent.Lore> history = new ArrayList<>();
        for (CodexContent.Lore l : content.lore) {
            if (l.id != null && l.id.startsWith(HISTORY_PREFIX)) history.add(l);
        }
        while (history.size() > MAX_HISTORY) {
            content.lore.remove(history.remove(0));
        }

        ContentManager.save();
        ContentManager.broadcast(server);
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
