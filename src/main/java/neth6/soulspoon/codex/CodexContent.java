package neth6.soulspoon.codex;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenido editable del Codex (lo que crean los admins desde el Editor).
 * Se guarda en config/soulspoon/codex.json y se sincroniza a los clientes como JSON.
 * Son POJOs simples para que Gson los lea/escriba sin codecs.
 */
public class CodexContent {
    public List<Quest> quests = new ArrayList<>();
    public List<Lore> lore = new ArrayList<>();
    public List<Mob> mobs = new ArrayList<>();
    /** IDs de recetas ocultas, ej. "minecraft:golden_apple". */
    public List<String> hiddenRecipes = new ArrayList<>();

    public static class Quest {
        public String id = "";
        public String category = "General";
        public String chapter = "Capítulo I";
        public String title = "";
        public String description = "";
        /** IDs de misiones que deben estar RECLAMADAS antes de poder hacer esta. */
        public List<String> requires = new ArrayList<>();
        /** KILL (matar mobs), OBTAIN (tener ítems en el inventario), DIMENSION (entrar a una dimensión). */
        public String objectiveType = "KILL";
        /** KILL: id de entidad. OBTAIN: id de ítem. DIMENSION: id de dimensión. */
        public String target = "";
        public int amount = 1;
        public String rewardItem = "";
        public int rewardCount = 1;
        /** Recompensa en deditas, expresada en azules (1 azul = 1, verde = 100, roja = 1.000, morada = 10.000). */
        public long rewardDeditas = 0;
        /** Misión diaria: su progreso y su "reclamada" se reinician cada día. */
        public boolean daily = false;
    }

    public static class Lore {
        public String id = "";
        public String title = "";
        public String text = "";
        /** "always" o "quest:<id_mision>" (se revela al reclamar esa misión). */
        public String reveal = "always";
    }

    public static class Mob {
        /** Clave única de la entrada, ej. "bomba_warden" (o "minecraft:zombie" si no hay variantes). */
        public String id = "";
        /** Entidad base para el modelo 3D y para descubrirlo al matar. Vacío = usar "id". */
        public String entity = "";
        /** Si no está vacío, solo se descubre al matar una entidad con ESTE nombre personalizado. */
        public String customName = "";
        public String name = "";
        public String danger = "Bajo";
        public String description = "";
        public String tips = "";
        public List<String> loot = new ArrayList<>();
        /** "always" o "kill" (se descubre al matar uno). */
        public String discover = "kill";
    }
}
