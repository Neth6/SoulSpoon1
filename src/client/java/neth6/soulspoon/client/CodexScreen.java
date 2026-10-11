package neth6.soulspoon.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import neth6.soulspoon.codex.CodexContent;
import neth6.soulspoon.network.CodexPackets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla principal del Codex. Cada pestaña tiene una lista a la izquierda
 * (con scroll de rueda) y el detalle a la derecha.
 */
public class CodexScreen extends Screen {
    private static final String[] TABS = {"Recetas", "Misiones", "Historia", "Bestiario"};
    private static final int W = 390, H = 250;
    private static final int ROWS = 6, ROW_H = 17;

    private static final int GOLD = 0xFFE7C98A, TEXT = 0xFFE8E2D8, DIM = 0xFF8C8478;

    private static int selectedTab = 1;               // recordamos la pestaña entre aperturas
    private final int[] scroll = new int[4];
    private final int[] selected = new int[4];

    private final Map<String, LivingEntity> entityCache = new HashMap<>();

    public CodexScreen() {
        super(Component.literal("Codex de aventuras"));
    }

    private int px() { return (width - W) / 2; }
    private int py() { return (height - H) / 2; }

    // ------------------------------------------------------------------ datos por pestaña

    /** Filtro de categoría de la pestaña Misiones ("" = todas). */
    private String category = "";

    private List<CodexContent.Quest> quests() {
        if (category.isEmpty()) return ClientCodexCache.content.quests;
        List<CodexContent.Quest> out = new ArrayList<>();
        for (CodexContent.Quest q : ClientCodexCache.content.quests) {
            if (q.category.equals(category)) out.add(q);
        }
        return out;
    }

    private List<String> categories() {
        List<String> out = new ArrayList<>();
        out.add("");
        for (CodexContent.Quest q : ClientCodexCache.content.quests) {
            if (!out.contains(q.category)) out.add(q.category);
        }
        return out;
    }

    private List<CodexContent.Lore> visibleLore() {
        List<CodexContent.Lore> out = new ArrayList<>();
        for (CodexContent.Lore l : ClientCodexCache.content.lore) {
            if (l.reveal == null || l.reveal.equals("always")
                    || (l.reveal.startsWith("quest:") && ClientCodexCache.claimed.contains(l.reveal.substring(6)))) {
                out.add(l);
            }
        }
        return out;
    }

    private List<CodexContent.Mob> mobs() {
        return ClientCodexCache.content.mobs;
    }

    private boolean mobKnown(CodexContent.Mob m) {
        return "always".equals(m.discover) || ClientCodexCache.discoveredMobs.contains(m.id);
    }

    private List<RecipeHolder<CraftingRecipe>> recipes() {
        List<RecipeHolder<CraftingRecipe>> out = new ArrayList<>();
        if (minecraft == null || minecraft.level == null) return out;
        // Recetas "personalizadas" = cualquier namespace que no sea minecraft (mod/datapack),
        // salvo las ocultas desde el editor.
        for (RecipeHolder<CraftingRecipe> h : minecraft.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            ResourceLocation id = h.id();
            if (id.getNamespace().equals("minecraft")) continue;
            if (ClientCodexCache.content.hiddenRecipes.contains(id.toString())) continue;
            out.add(h);
        }
        return out;
    }

    private int listSize() {
        return switch (selectedTab) {
            case 0 -> recipes().size();
            case 1 -> quests().size();
            case 2 -> visibleLore().size();
            default -> mobs().size();
        };
    }

    private String rowLabel(int index) {
        return switch (selectedTab) {
            case 0 -> recipes().get(index).value().getResultItem(minecraft.level.registryAccess())
                    .getHoverName().getString();
            case 1 -> (quests().get(index).daily ? "[D] " : "") + quests().get(index).title;
            case 2 -> visibleLore().get(index).title;
            default -> mobKnown(mobs().get(index)) ? mobs().get(index).name : "???";
        };
    }

    // ------------------------------------------------------------------ widgets

    @Override
    protected void init() {
        ClientCodexCache.requestSync();

        int x = px(), y = py();

        for (int i = 0; i < TABS.length; i++) {
            final int tab = i;
            Button b = Button.builder(Component.literal(TABS[i]), btn -> {
                selectedTab = tab;
                entityCache.clear();
                rebuildWidgets();
            }).bounds(x + 12 + i * 92, y + 43, 88, 22).build();
            b.active = i != selectedTab;
            addRenderableWidget(b);
        }

        // Filas de la lista (máx. ROWS visibles).
        int size = listSize();
        scroll[selectedTab] = Mth.clamp(scroll[selectedTab], 0, Math.max(0, size - ROWS));
        for (int r = 0; r < ROWS; r++) {
            int index = scroll[selectedTab] + r;
            if (index >= size) break;
            final int idx = index;
            Button row = Button.builder(Component.literal(trim(rowLabel(index), 20)), btn -> {
                selected[selectedTab] = idx;
                entityCache.clear();
                rebuildWidgets();
            }).bounds(x + 12, y + 80 + r * ROW_H, 132, ROW_H - 1).build();
            row.active = index != selected[selectedTab];
            addRenderableWidget(row);
        }

        // Filtro de categoría (solo pestaña de misiones).
        if (selectedTab == 1) {
            addRenderableWidget(Button.builder(
                    Component.literal("Categoría: " + (category.isEmpty() ? "Todas" : trim(category, 12))),
                    btn -> {
                        List<String> cats = categories();
                        category = cats.get((cats.indexOf(category) + 1) % cats.size());
                        selected[1] = 0;
                        scroll[1] = 0;
                        rebuildWidgets();
                    }).bounds(x + 12, y + 184, 132, 18).build());
        }

        // Botón de reclamar (solo pestaña de misiones).
        if (selectedTab == 1 && selected[1] < quests().size()) {
            CodexContent.Quest q = quests().get(selected[1]);
            int prog = ClientCodexCache.progress.getOrDefault(q.id, 0);
            boolean claimed = ClientCodexCache.claimed.contains(q.id);
            boolean canClaim = !claimed && ClientCodexCache.isUnlocked(q) && prog >= q.amount;
            Button claim = Button.builder(
                    Component.literal(claimed ? "Reclamada" : "Reclamar recompensa"),
                    btn -> {
                        ClientPlayNetworking.send(new CodexPackets.ClaimQuestC2S(q.id));
                    }).bounds(x + 160, y + 186, 124, 20).build();
            claim.active = canClaim;
            addRenderableWidget(claim);
        }

        // Editor: visible solo con OP. El servidor revalida cada guardado.
        if (minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2)) {
            addRenderableWidget(Button.builder(Component.literal("Editor"), b ->
                            minecraft.setScreen(new CodexEditorScreen(this)))
                    .bounds(x + 12, y + 211, 85, 22).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(x + 293, y + 211, 85, 22).build());
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int x = px(), y = py();
        if (mx >= x + 12 && mx <= x + 144 && my >= y + 80 && my <= y + 80 + ROWS * ROW_H) {
            int max = Math.max(0, listSize() - ROWS);
            int next = Mth.clamp(scroll[selectedTab] - (int) Math.signum(sy), 0, max);
            if (next != scroll[selectedTab]) {
                scroll[selectedTab] = next;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // ------------------------------------------------------------------ render

    /**
     * En 1.21 Screen.render() ya llama a renderBackground(), y el de vanilla aplica BLUR.
     * Si dibujamos el panel antes y dejamos que vanilla vuelva a llamarlo, el blur se aplica
     * encima del panel y del título. Solución: sobrescribirlo, usar fondo oscuro SIN blur y
     * dibujar aquí el panel (así queda debajo de los botones y nítido).
     */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderTransparentBackground(g);   // oscurece el mundo, sin desenfoque
        int x = px(), y = py();

        g.fill(x, y, x + W, y + H, 0xF21B1820);
        g.fill(x, y, x + W, y + 3, GOLD);
        g.fill(x, y, x + 3, y + H, GOLD);
        g.fill(x + W - 3, y, x + W, y + H, GOLD);
        g.drawCenteredString(font, Component.literal("SOULSPOON • CODEX DE AVENTURAS"), width / 2, y + 15, GOLD);
        g.fill(x + 12, y + 72, x + W - 12, y + 73, 0xFF665344);
        g.fill(x + 150, y + 78, x + 151, y + 205, 0xFF665344); // separador lista | detalle
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int x = px(), y = py();

        super.render(g, mouseX, mouseY, delta);   // fondo (renderBackground) + botones

        int size = listSize();
        if (size == 0) {
            g.drawString(font, "Sin entradas todavía.", x + 160, y + 85, DIM, false);
            return;
        }
        int sel = Mth.clamp(selected[selectedTab], 0, size - 1);
        int dx = x + 160;
        int dy = y + 82;

        switch (selectedTab) {
            case 0 -> renderRecipe(g, dx, dy, sel);
            case 1 -> renderQuest(g, dx, dy, sel);
            case 2 -> renderLore(g, dx, dy, sel);
            default -> renderMob(g, dx, dy, sel, mouseX, mouseY);
        }
    }

    // ---- Recetas

    private void renderRecipe(GuiGraphics g, int dx, int dy, int sel) {
        RecipeHolder<CraftingRecipe> holder = recipes().get(sel);
        CraftingRecipe recipe = holder.value();
        ItemStack result = recipe.getResultItem(minecraft.level.registryAccess());

        g.drawString(font, result.getHoverName(), dx, dy, GOLD, false);
        g.drawString(font, holder.id().toString(), dx, dy + 11, DIM, false);

        int gridX = dx + 8, gridY = dy + 32;
        int recipeW = recipe instanceof ShapedRecipe s ? s.getWidth() : 3;
        List<Ingredient> ings = recipe.getIngredients();
        // Los ingredientes alternativos rotan cada segundo.
        int tick = (int) (System.currentTimeMillis() / 1000);

        for (int i = 0; i < 9; i++) {
            int cx = gridX + (i % 3) * 20;
            int cy = gridY + (i / 3) * 20;
            g.fill(cx, cy, cx + 18, cy + 18, 0x66000000);
        }
        for (int i = 0; i < ings.size(); i++) {
            ItemStack[] options = ings.get(i).getItems();
            if (options.length == 0) continue;
            int col = i % recipeW, row = i / recipeW;
            if (col > 2 || row > 2) continue;
            g.renderItem(options[tick % options.length], gridX + col * 20 + 1, gridY + row * 20 + 1);
        }
        g.drawString(font, "→", gridX + 68, gridY + 25, TEXT, false);
        g.fill(gridX + 82, gridY + 19, gridX + 102, gridY + 39, 0x66000000);
        g.renderItem(result, gridX + 84, gridY + 21);
        g.renderItemDecorations(font, result, gridX + 84, gridY + 21);
    }

    // ---- Misiones

    private void renderQuest(GuiGraphics g, int dx, int dy, int sel) {
        CodexContent.Quest q = quests().get(sel);
        int prog = ClientCodexCache.progress.getOrDefault(q.id, 0);
        boolean claimed = ClientCodexCache.claimed.contains(q.id);
        boolean unlocked = ClientCodexCache.isUnlocked(q);

        g.drawString(font, q.chapter + " • " + q.category + (q.daily ? " • DIARIA" : ""), dx, dy, DIM, false);
        g.drawString(font, q.title, dx, dy + 12, GOLD, false);
        if (q.daily) {
            String reset = "Reinicia en " + ClientCodexCache.resetText();
            g.drawString(font, reset, dx + 215 - font.width(reset), dy + 12, 0xFF7FB7E6, false);
        }

        if (!unlocked) {
            g.drawString(font, "[ Bloqueada ]", dx, dy + 28, 0xFFE06060, false);
            StringBuilder missing = new StringBuilder();
            for (String r : q.requires) {
                if (!ClientCodexCache.claimed.contains(r)) {
                    if (!missing.isEmpty()) missing.append(", ");
                    missing.append(titleOfQuest(r));
                }
            }
            drawWrapped(g, "Completa primero: " + missing, dx, dy + 42, 215, TEXT);
            return;
        }

        drawWrapped(g, q.description, dx, dy + 28, 215, TEXT, 4);

        int by = dy + 64;
        g.drawString(font, objectiveLabel(q), dx, by, TEXT, false);
        String counter = prog + " / " + q.amount;
        g.drawString(font, counter, dx + 215 - font.width(counter), by, DIM, false);
        int barW = 215;
        float pct = Mth.clamp(prog / (float) q.amount, 0f, 1f);
        g.fill(dx, by + 12, dx + barW, by + 20, 0xFF3A3530);
        g.fill(dx, by + 12, dx + (int) (barW * pct), by + 20, claimed ? 0xFF6AA84F : 0xFF55CC55);

        if (!q.rewardItem.isBlank()) {
            ResourceLocation rl = ResourceLocation.tryParse(q.rewardItem);
            if (rl != null) {
                BuiltInRegistries.ITEM.getOptional(rl).ifPresent(item -> {
                    ItemStack stack = new ItemStack(item, q.rewardCount);
                    g.drawString(font, "Recompensa:", dx, dy + 94, TEXT, false);
                    g.renderItem(stack, dx + 66, dy + 90);
                    g.renderItemDecorations(font, stack, dx + 66, dy + 90);
                    g.drawString(font, stack.getHoverName(), dx + 88, dy + 94, GOLD, false);
                });
            }
        }
    }

    private String titleOfQuest(String id) {
        for (CodexContent.Quest q : ClientCodexCache.content.quests) if (q.id.equals(id)) return q.title;
        return id;
    }

    private String objectiveLabel(CodexContent.Quest q) {
        String target = q.target;
        ResourceLocation rl = ResourceLocation.tryParse(q.target);
        if (rl != null) {
            switch (q.objectiveType) {
                case "KILL" -> target = BuiltInRegistries.ENTITY_TYPE.getOptional(rl)
                        .map(t -> t.getDescription().getString()).orElse(q.target);
                case "OBTAIN" -> target = BuiltInRegistries.ITEM.getOptional(rl)
                        .map(i -> i.getDescription().getString()).orElse(q.target);
                default -> { }
            }
        }
        return switch (q.objectiveType) {
            case "KILL" -> "Derrota: " + target;
            case "OBTAIN" -> "Consigue: " + target;
            default -> "Viaja a: " + q.target;
        };
    }

    // ---- Historia

    private void renderLore(GuiGraphics g, int dx, int dy, int sel) {
        CodexContent.Lore l = visibleLore().get(sel);
        g.drawString(font, l.title, dx, dy, GOLD, false);
        drawWrapped(g, l.text, dx, dy + 16, 215, TEXT);
    }

    // ---- Bestiario

    private void renderMob(GuiGraphics g, int dx, int dy, int sel, int mouseX, int mouseY) {
        CodexContent.Mob m = mobs().get(sel);
        if (!mobKnown(m)) {
            g.drawString(font, "???", dx, dy, GOLD, false);
            drawWrapped(g, "Criatura no descubierta. Derrota a una para registrarla.", dx, dy + 16, 215, DIM);
            return;
        }
        g.drawString(font, m.name, dx, dy, GOLD, false);
        g.drawString(font, "Peligro: " + m.danger, dx, dy + 12, dangerColor(m.danger), false);

        // Modelo 3D a la derecha.
        LivingEntity living = entityFor(m);
        if (living != null) {
            int bx = dx + 150;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, bx, dy, bx + 66, dy + 66, 28, 0.0625f, mouseX, mouseY, living);
        }

        drawWrapped(g, m.description, dx, dy + 28, 140, TEXT);
        int ty = dy + 28 + wrappedHeight(m.description, 140) + 6;
        g.drawString(font, "Qué hacer:", dx, ty, GOLD, false);
        drawWrapped(g, m.tips, dx, ty + 11, 215, TEXT);
        int ly = ty + 11 + wrappedHeight(m.tips, 215) + 6;

        if (!m.loot.isEmpty() && ly < dy + 112) {
            g.drawString(font, "Botín:", dx, ly + 4, GOLD, false);
            int ix = dx + 38;
            for (String lootId : m.loot) {
                ResourceLocation rl = ResourceLocation.tryParse(lootId);
                if (rl == null) continue;
                var item = BuiltInRegistries.ITEM.getOptional(rl);
                if (item.isEmpty()) continue;
                g.renderItem(new ItemStack(item.get()), ix, ly);
                ix += 18;
            }
        }
    }

    private LivingEntity entityFor(CodexContent.Mob m) {
        if (minecraft == null || minecraft.level == null) return null;
        String base = (m.entity == null || m.entity.isBlank()) ? m.id : m.entity;
        return entityCache.computeIfAbsent(m.id, id -> {
            ResourceLocation rl = ResourceLocation.tryParse(base);
            if (rl == null) return null;
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
            if (type.isEmpty()) return null;
            Entity e = type.get().create(minecraft.level);
            return e instanceof LivingEntity le ? le : null;
        });
    }

    private static int dangerColor(String danger) {
        String d = danger == null ? "" : danger.toLowerCase();
        if (d.contains("extremo") || d.contains("muy")) return 0xFFFF5555;
        if (d.contains("alto")) return 0xFFFF9944;
        if (d.contains("medio")) return 0xFFFFDD55;
        return 0xFF77DD77;
    }

    // ------------------------------------------------------------------ utilidades

    private void drawWrapped(GuiGraphics g, String text, int x, int y, int maxW, int color) {
        drawWrapped(g, text, x, y, maxW, color, 9);
    }

    private void drawWrapped(GuiGraphics g, String text, int x, int y, int maxW, int color, int maxLines) {
        int line = 0;
        for (var seq : font.split(Component.literal(text), maxW)) {
            g.drawString(font, seq, x, y + line * 10, color, false);
            line++;
            if (line >= maxLines) break;
        }
    }

    private int wrappedHeight(String text, int maxW) {
        return Math.min(9, font.split(Component.literal(text), maxW).size()) * 10;
    }

    private String trim(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
