package neth6.soulspoon.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import neth6.soulspoon.codex.CodexContent;
import neth6.soulspoon.network.CodexPackets;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor genérico: cada sección define sus campos; se serializan a UNA entrada JSON
 * que se manda al servidor. El servidor valida permisos y contenido.
 */
public class CodexEditorScreen extends Screen {
    private record Field(String key, String hint, Kind kind) {}
    private enum Kind { TEXT, INT, LIST, BOOL }

    private static final String[] SECTION_KEYS = {"quest", "lore", "mob", "recipe"};
    private static final String[] SECTION_NAMES = {"Misión", "Lore", "Bestiario", "Recetas"};

    private static final Field[][] FIELDS = {
            { // quest
                    new Field("id", "ID único (primera_sangre)", Kind.TEXT),
                    new Field("category", "Categoría (Combate)", Kind.TEXT),
                    new Field("chapter", "Capítulo", Kind.TEXT),
                    new Field("title", "Título", Kind.TEXT),
                    new Field("description", "Descripción", Kind.TEXT),
                    new Field("requires", "Requiere IDs (a,b)", Kind.LIST),
                    new Field("objectiveType", "KILL / OBTAIN / DIMENSION", Kind.TEXT),
                    new Field("target", "Objetivo (minecraft:zombie)", Kind.TEXT),
                    new Field("amount", "Cantidad", Kind.INT),
                    new Field("rewardItem", "Ítem recompensa", Kind.TEXT),
                    new Field("rewardCount", "Cant. recompensa", Kind.INT),
                    new Field("daily", "Diaria (true/false)", Kind.BOOL),
            },
            { // lore
                    new Field("id", "ID único", Kind.TEXT),
                    new Field("title", "Título", Kind.TEXT),
                    new Field("text", "Texto de la historia", Kind.TEXT),
                    new Field("reveal", "always / quest:<id>", Kind.TEXT),
            },
            { // mob
                    new Field("id", "ID único (bomba_warden)", Kind.TEXT),
                    new Field("entity", "Entidad base (minecraft:phantom)", Kind.TEXT),
                    new Field("customName", "Nombre custom a matar (opcional)", Kind.TEXT),
                    new Field("name", "Nombre visible", Kind.TEXT),
                    new Field("danger", "Peligro (Bajo/Medio/Alto/Extremo)", Kind.TEXT),
                    new Field("description", "Descripción", Kind.TEXT),
                    new Field("tips", "Qué hacer", Kind.TEXT),
                    new Field("loot", "Botín (ítems separados por coma)", Kind.LIST),
                    new Field("discover", "always / kill", Kind.TEXT),
            },
            { // recipe
                    new Field("id", "ID de receta a ocultar (minecraft:golden_apple)", Kind.TEXT),
            }
    };

    private final Screen parent;
    private int section = 0;
    private final List<EditBox> boxes = new ArrayList<>();

    public CodexEditorScreen(Screen parent) {
        super(Component.literal("Editor del Codex"));
        this.parent = parent;
    }

    private int px() { return (width - 360) / 2; }
    private int py() { return (height - 236) / 2; }

    @Override
    protected void init() {
        boxes.clear();
        int x = px(), y = py();

        for (int i = 0; i < SECTION_NAMES.length; i++) {
            final int s = i;
            Button b = Button.builder(Component.literal(SECTION_NAMES[i]), btn -> {
                section = s;
                ClientCodexCache.editorMessage = "";
                rebuildWidgets();
            }).bounds(x + 8 + i * 88, y + 8, 84, 20).build();
            b.active = i != section;
            addRenderableWidget(b);
        }

        Field[] fields = FIELDS[section];
        for (int i = 0; i < fields.length; i++) {
            int cx = x + 8 + (i % 2) * 175;
            int cy = y + 40 + (i / 2) * 24;
            int w = fields.length == 1 ? 342 : 169;
            EditBox box = new EditBox(font, cx, cy, w, 20, Component.literal(fields[i].hint()));
            // Por defecto EditBox limita a 32 caracteres. Los límites suman <12000 chars en el peor caso
            // (cabe en el paquete C2S de 32 KiB y en el codec de EditorSaveC2S).
            String key = fields[i].key();
            boolean longText = key.equals("description") || key.equals("text") || key.equals("tips");
            box.setMaxLength(longText ? 1500 : 200);
            box.setHint(Component.literal(fields[i].hint()));
            addRenderableWidget(box);
            boxes.add(box);
        }

        int by = y + 206;
        addRenderableWidget(Button.builder(Component.literal("Cargar ID"), b -> loadExisting())
                .bounds(x + 8, by, 80, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Guardar"), b -> send(false))
                .bounds(x + 92, by, 80, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Borrar"), b -> send(true))
                .bounds(x + 176, by, 80, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Volver"), b -> onClose())
                .bounds(x + 270, by, 80, 22).build());
    }

    // ------------------------------------------------------------------ enviar

    private void send(boolean delete) {
        Field[] fields = FIELDS[section];
        JsonObject obj = new JsonObject();
        for (int i = 0; i < fields.length; i++) {
            String value = boxes.get(i).getValue().trim();
            switch (fields[i].kind()) {
                case INT -> {
                    int n = 1;
                    try { n = Integer.parseInt(value); } catch (NumberFormatException ignored) { }
                    obj.addProperty(fields[i].key(), n);
                }
                case BOOL -> obj.addProperty(fields[i].key(), Boolean.parseBoolean(value));
                case LIST -> {
                    JsonArray arr = new JsonArray();
                    for (String part : value.split(",")) {
                        if (!part.isBlank()) arr.add(part.trim());
                    }
                    obj.add(fields[i].key(), arr);
                }
                default -> obj.addProperty(fields[i].key(), value);
            }
        }
        ClientCodexCache.editorMessage = "Enviando…";
        ClientCodexCache.editorOk = true;
        ClientPlayNetworking.send(new CodexPackets.EditorSaveC2S(SECTION_KEYS[section], obj.toString(), delete));
    }

    /** Rellena los campos con la entrada existente cuyo ID está escrito en el primer campo. */
    private void loadExisting() {
        String id = boxes.get(0).getValue().trim();
        CodexContent c = ClientCodexCache.content;
        Object found = null;
        switch (section) {
            case 0 -> found = c.quests.stream().filter(q -> q.id.equals(id)).findFirst().orElse(null);
            case 1 -> found = c.lore.stream().filter(l -> l.id.equals(id)).findFirst().orElse(null);
            case 2 -> found = c.mobs.stream().filter(m -> m.id.equals(id)).findFirst().orElse(null);
            default -> { }
        }
        if (found == null) {
            ClientCodexCache.editorOk = false;
            ClientCodexCache.editorMessage = "No existe ese ID";
            return;
        }
        JsonObject obj = new Gson().toJsonTree(found).getAsJsonObject();
        Field[] fields = FIELDS[section];
        for (int i = 0; i < fields.length; i++) {
            JsonElement e = obj.get(fields[i].key());
            if (e == null) continue;
            if (e.isJsonArray()) {
                List<String> parts = new ArrayList<>();
                e.getAsJsonArray().forEach(p -> parts.add(p.getAsString()));
                boxes.get(i).setValue(String.join(",", parts));
            } else {
                boxes.get(i).setValue(e.getAsString());
            }
        }
        ClientCodexCache.editorOk = true;
        ClientCodexCache.editorMessage = "Cargado";
    }

    // ------------------------------------------------------------------ render

    /** Fondo sin blur: ver explicación en CodexScreen.renderBackground. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderTransparentBackground(g);
        int x = px(), y = py();
        g.fill(x, y, x + 360, y + 236, 0xF21B1820);
        g.fill(x, y, x + 360, y + 3, 0xFFE7C98A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int x = px(), y = py();
        super.render(g, mouseX, mouseY, delta);   // fondo + widgets

        g.drawString(font, "EDITOR • " + SECTION_NAMES[section].toUpperCase(), x + 8, y + 30, 0xFFE7C98A, false);
        if (!ClientCodexCache.editorMessage.isEmpty()) {
            g.drawString(font, ClientCodexCache.editorMessage, x + 8, y + 193,
                    ClientCodexCache.editorOk ? 0xFF77DD77 : 0xFFFF6666, false);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
