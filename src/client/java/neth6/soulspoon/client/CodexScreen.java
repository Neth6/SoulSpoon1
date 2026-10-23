package neth6.soulspoon.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
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
import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.codex.CodexContent;
import neth6.soulspoon.network.CodexPackets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Codex con el marco plateado (codex_panel.png, 280x164). Los 4 botones de la izquierda son las
 * 4 secciones. Todo se dibuja en "unidades de textura" y el conjunto se escala (k) a la pantalla,
 * asi el pixel-art siempre queda nitido y centrado.
 */
public class CodexScreen extends Screen {
    // Secciones (indices internos): 0 Recetas, 1 Misiones, 2 Historia, 3 Bestiario.
    private static final String[] TABS = {"Recetas", "Misiones", "Historia", "Bestiario"};
    // Boton de arriba a abajo (libro, espada, pergamino, lupa) -> seccion. CAMBIA AQUI si quieres otro orden.
    private static final int[] BUTTON_TAB = {0, 3, 1, 2};
    private static final String[] BUTTON_KIND = {"libro", "espada", "pergamino", "lupa"};

    private static final int PANEL_W = 280, PANEL_H = 164;
    private static final int BTN_X = 6, BTN_Y0 = 9, BTN_PITCH = 37, BTN_SIZE = 34;
    private static final int LIST_X = 50, LIST_Y = 26, LIST_W = 68, ROW_H = 14, ROWS = 7;
    private static final int DET_X = 125, DET_Y = 26, DET_W = 131;
    private static final int BOT_Y = 134, BOT_H = 14;
    private static final int TRACK_X = 262, TRACK_Y = 10, TRACK_H = 145;
    private static final float T = 0.75f; // escala del texto

    private static final int GOLD = 0xFFE7C98A, TEXT = 0xFFE8E2D8, DIM = 0xFF9C94B0;
    private static final int[] WHEEL = {0xFFE5484D, 0xFF9B5DE5, 0xFF3FBF6A, 0xFFF2C94C,
            0xFF2EC4D6, 0xFF3A7BFF, 0xFFF28C28};
    private static final int[] TAB_COLORS = {WHEEL[6], WHEEL[5], WHEEL[1], WHEEL[0]};

    private static final ResourceLocation PANEL = SoulSpoon.id("textures/gui/codex_panel.png");
    private static final ResourceLocation THUMB = SoulSpoon.id("textures/gui/scroll_thumb.png");
    private static final ResourceLocation[] BTN = new ResourceLocation[4];
    private static final ResourceLocation[] BTN_HOVER = new ResourceLocation[4];

    static {
        for (int i = 0; i < 4; i++) {
            BTN[i] = SoulSpoon.id("textures/gui/boton_" + BUTTON_KIND[i] + ".png");
            BTN_HOVER[i] = SoulSpoon.id("textures/gui/boton_" + BUTTON_KIND[i] + "_hover.png");
        }
    }

    private static int selectedTab = 1;
    private final int[] scroll = new int[4];
    private final int[] selected = new int[4];
    private String category = "";
    private boolean draggingBar = false;
    private final Map<String, LivingEntity> entityCache = new HashMap<>();

    public CodexScreen() {
        super(Component.literal("Codex de aventuras"));
    }

    @Override
    protected void init() {
        ClientCodexCache.requestSync();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ geometria

    private float k() {
        float fit = Math.min((width - 8) / (float) PANEL_W, (height - 8) / (float) PANEL_H);
        return Math.max(1f, (float) Math.floor(fit * 2f) / 2f);
    }

    private float x0(float k) { return (width - PANEL_W * k) / 2f; }
    private float y0(float k) { return (height - PANEL_H * k) / 2f; }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ datos

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

    private boolean rowDone(int index) {
        return selectedTab == 1 && ClientCodexCache.claimed.contains(quests().get(index).id);
    }

    private int maxScroll() {
        return Math.max(0, listSize() - ROWS);
    }

    private boolean canEdit() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    // rectangulos de los botones inferiores
    private boolean hasCategory() { return selectedTab == 1; }
    private boolean hasClaim() { return selectedTab == 1 && selected[1] < quests().size(); }

    private boolean canClaim() {
        if (!hasClaim()) return false;
        CodexContent.Quest q = quests().get(selected[1]);
        int prog = ClientCodexCache.progress.getOrDefault(q.id, 0);
        return !ClientCodexCache.claimed.contains(q.id) && ClientCodexCache.isUnlocked(q) && prog >= q.amount;
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        float k = k();
        double lx = (mx - x0(k)) / k, ly = (my - y0(k)) / k;

        // 4 botones de seccion
        for (int i = 0; i < 4; i++) {
            if (in(lx, ly, BTN_X, BTN_Y0 + BTN_PITCH * i, BTN_SIZE, BTN_SIZE)) {
                selectedTab = BUTTON_TAB[i];
                entityCache.clear();
                return true;
            }
        }
        // filas
        int size = listSize();
        for (int r = 0; r < ROWS; r++) {
            int index = scroll[selectedTab] + r;
            if (index >= size) break;
            if (in(lx, ly, LIST_X, LIST_Y + r * ROW_H, LIST_W, ROW_H - 1)) {
                selected[selectedTab] = index;
                entityCache.clear();
                return true;
            }
        }
        // barra de scroll
        if (size > ROWS && in(lx, ly, TRACK_X - 4, TRACK_Y, 10, TRACK_H)) {
            draggingBar = true;
            dragTo(ly);
            return true;
        }
        // categoria
        if (hasCategory() && in(lx, ly, LIST_X, BOT_Y, LIST_W, BOT_H)) {
            List<String> cats = categories();
            category = cats.get((cats.indexOf(category) + 1) % cats.size());
            selected[1] = 0;
            scroll[1] = 0;
            return true;
        }
        // reclamar
        if (hasClaim() && canClaim() && in(lx, ly, DET_X, BOT_Y, 92, BOT_H)) {
            ClientPlayNetworking.send(new CodexPackets.ClaimQuestC2S(quests().get(selected[1]).id));
            return true;
        }
        // editor (OP)
        if (canEdit() && in(lx, ly, DET_X + DET_W - 36, BOT_Y, 36, BOT_H)) {
            minecraft.setScreen(new CodexEditorScreen(this));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void dragTo(double ly) {
        int max = maxScroll();
        if (max <= 0) return;
        double f = Mth.clamp((ly - TRACK_Y) / TRACK_H, 0, 1);
        scroll[selectedTab] = Mth.clamp((int) Math.round(f * max), 0, max);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingBar) {
            float k = k();
            dragTo((my - y0(k)) / k);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingBar = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int max = maxScroll();
        scroll[selectedTab] = Mth.clamp(scroll[selectedTab] - (int) Math.signum(sy), 0, max);
        return true;
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderTransparentBackground(g); // oscurece el mundo sin desenfoque
        float k = k();
        float ox = x0(k), oy = y0(k);
        int lmx = (int) ((mouseX - ox) / k), lmy = (int) ((mouseY - oy) / k);

        int size = listSize();
        scroll[selectedTab] = Mth.clamp(scroll[selectedTab], 0, maxScroll());
        if (size > 0) selected[selectedTab] = Mth.clamp(selected[selectedTab], 0, size - 1);

        g.pose().pushPose();
        g.pose().translate(ox, oy, 0);
        g.pose().scale(k, k, 1f);

        g.blit(PANEL, 0, 0, PANEL_W, PANEL_H, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        // botones de seccion
        int hoverButton = -1;
        for (int i = 0; i < 4; i++) {
            int by = BTN_Y0 + BTN_PITCH * i;
            boolean hover = in(lmx, lmy, BTN_X, by, BTN_SIZE, BTN_SIZE);
            if (hover) hoverButton = i;
            boolean on = BUTTON_TAB[i] == selectedTab;
            g.blit(hover || on ? BTN_HOVER[i] : BTN[i], BTN_X, by, BTN_SIZE, BTN_SIZE,
                    0, 0, BTN_SIZE, BTN_SIZE, BTN_SIZE, BTN_SIZE);
            if (on) { // marca de color a la derecha del boton activo
                g.fill(BTN_X + BTN_SIZE + 1, by + 4, BTN_X + BTN_SIZE + 3, by + BTN_SIZE - 4, TAB_COLORS[BUTTON_TAB[i]]);
            }
        }

        // cabecera: titulo de la seccion + saldo de deditas
        g.fill(48, 13, 258, 23, 0xCC140B1E);
        g.fill(48, 23, 258, 24, TAB_COLORS[selectedTab]);
        text(g, TABS[selectedTab].toUpperCase(), 52, 16, GOLD);
        long total = EconomyHud.total();
        int aw = (int) Math.ceil(EconomyHud.amountWidth(font, total) * T);
        amount(g, 254 - aw, 16, total, TEXT);
        String dl = "Deditas";
        text(g, dl, 254 - aw - 4 - tw(dl), 16, DIM);

        // lista
        g.fill(48, 25, 120, 25 + ROWS * ROW_H + 2, 0xCC140B1E);
        for (int r = 0; r < ROWS; r++) {
            int index = scroll[selectedTab] + r;
            if (index >= size) break;
            int ry = LIST_Y + r * ROW_H;
            int accent = WHEEL[index % WHEEL.length];
            boolean sel = index == selected[selectedTab];
            boolean hover = in(lmx, lmy, LIST_X, ry, LIST_W, ROW_H - 1);
            g.fill(LIST_X, ry, LIST_X + LIST_W, ry + ROW_H - 1, sel ? shade(accent, 0.55f) : hover ? shade(accent, 0.30f) : 0xFF241E38);
            g.fill(LIST_X, ry, LIST_X + 2, ry + ROW_H - 1, accent);
            String label = font.plainSubstrByWidth(rowLabel(index), (int) ((LIST_W - 8) / T));
            text(g, label, LIST_X + 5, ry + 3, sel ? 0xFFFFFFFF : rowDone(index) ? 0xFF7FD99A : TEXT);
        }

        // barra de scroll
        if (size > ROWS) {
            int th = Math.max(12, TRACK_H * ROWS / size);
            int ty = TRACK_Y + (TRACK_H - th) * scroll[selectedTab] / maxScroll();
            g.blit(THUMB, TRACK_X, ty, 2, th, 0, 0, 2, 58, 2, 58);
        }

        // detalle
        g.fill(123, 25, 258, 131, 0xCC140B1E);
        if (size == 0) {
            text(g, "Sin entradas todavía.", DET_X + 2, DET_Y + 2, DIM);
        } else {
            int sel = selected[selectedTab];
            switch (selectedTab) {
                case 0 -> renderRecipe(g, DET_X, DET_Y, sel);
                case 1 -> renderQuest(g, DET_X, DET_Y, sel);
                case 2 -> renderLore(g, DET_X, DET_Y, sel);
                default -> renderMob(g, DET_X, DET_Y, sel, lmx, lmy);
            }
        }

        // botones inferiores
        if (hasCategory()) {
            button(g, LIST_X, BOT_Y, LIST_W, BOT_H, "Cat: " + (category.isEmpty() ? "Todas" : category),
                    WHEEL[3], true, in(lmx, lmy, LIST_X, BOT_Y, LIST_W, BOT_H));
        }
        if (hasClaim()) {
            CodexContent.Quest q = quests().get(selected[1]);
            boolean claimed = ClientCodexCache.claimed.contains(q.id);
            button(g, DET_X, BOT_Y, 92, BOT_H, claimed ? "Reclamada" : "Reclamar recompensa",
                    WHEEL[2], canClaim(), in(lmx, lmy, DET_X, BOT_Y, 92, BOT_H));
        }
        if (canEdit()) {
            button(g, DET_X + DET_W - 36, BOT_Y, 36, BOT_H, "Editor", WHEEL[4], true,
                    in(lmx, lmy, DET_X + DET_W - 36, BOT_Y, 36, BOT_H));
        }

        g.pose().popPose();

        if (hoverButton >= 0) {
            g.renderTooltip(font, Component.literal(TABS[BUTTON_TAB[hoverButton]]), mouseX, mouseY);
        }
    }

    // ---- recetas

    private void renderRecipe(GuiGraphics g, int dx, int dy, int sel) {
        RecipeHolder<CraftingRecipe> holder = recipes().get(sel);
        CraftingRecipe recipe = holder.value();
        ItemStack result = recipe.getResultItem(minecraft.level.registryAccess());

        text(g, result.getHoverName(), dx + 2, dy + 2, GOLD);
        text(g, font.plainSubstrByWidth(holder.id().toString(), (int) ((DET_W - 4) / T)), dx + 2, dy + 12, DIM);

        int gridX = dx + 6, gridY = dy + 30;
        int recipeW = recipe instanceof ShapedRecipe s ? s.getWidth() : 3;
        List<Ingredient> ings = recipe.getIngredients();
        int tick = (int) (System.currentTimeMillis() / 1000);

        for (int i = 0; i < 9; i++) {
            int cx = gridX + (i % 3) * 20, cy = gridY + (i / 3) * 20;
            g.fill(cx, cy, cx + 18, cy + 18, 0x66000000);
        }
        for (int i = 0; i < ings.size(); i++) {
            ItemStack[] options = ings.get(i).getItems();
            if (options.length == 0) continue;
            int col = i % recipeW, row = i / recipeW;
            if (col > 2 || row > 2) continue;
            g.renderItem(options[tick % options.length], gridX + col * 20 + 1, gridY + row * 20 + 1);
        }
        text(g, "->", gridX + 66, gridY + 26, TEXT);
        g.fill(gridX + 82, gridY + 20, gridX + 102, gridY + 40, 0x66000000);
        g.renderItem(result, gridX + 84, gridY + 22);
        g.renderItemDecorations(font, result, gridX + 84, gridY + 22);
    }

    // ---- misiones

    private void renderQuest(GuiGraphics g, int dx, int dy, int sel) {
        CodexContent.Quest q = quests().get(sel);
        int prog = ClientCodexCache.progress.getOrDefault(q.id, 0);
        boolean claimed = ClientCodexCache.claimed.contains(q.id);
        boolean unlocked = ClientCodexCache.isUnlocked(q);

        text(g, q.chapter + " • " + q.category + (q.daily ? " • DIARIA" : ""), dx + 2, dy + 2, DIM);
        text(g, q.title, dx + 2, dy + 12, GOLD);
        if (q.daily) {
            String reset = "Reinicia en " + ClientCodexCache.resetText();
            text(g, reset, dx + DET_W - 2 - tw(reset), dy + 22, 0xFF7FB7E6);
        }

        if (!unlocked) {
            text(g, "[ Bloqueada ]", dx + 2, dy + 34, 0xFFE06060);
            StringBuilder missing = new StringBuilder();
            for (String r : q.requires) {
                if (!ClientCodexCache.claimed.contains(r)) {
                    if (missing.length() > 0) missing.append(", ");
                    missing.append(titleOfQuest(r));
                }
            }
            wrapped(g, "Completa primero: " + missing, dx + 2, dy + 46, DET_W - 4, TEXT, 8);
            return;
        }

        wrapped(g, q.description, dx + 2, dy + 32, DET_W - 4, TEXT, 4);

        int by = dy + 68;
        text(g, objectiveLabel(q), dx + 2, by, TEXT);
        String counter = prog + " / " + q.amount;
        text(g, counter, dx + DET_W - 2 - tw(counter), by, DIM);
        int barW = DET_W - 4;
        float pct = Mth.clamp(prog / (float) Math.max(1, q.amount), 0f, 1f);
        g.fill(dx + 2, by + 10, dx + 2 + barW, by + 15, 0xFF3A3530);
        g.fill(dx + 2, by + 10, dx + 2 + (int) (barW * pct), by + 15, claimed ? 0xFF6AA84F : 0xFF55CC55);

        if (!q.rewardItem.isBlank() || q.rewardDeditas > 0) {
            text(g, "Recompensa:", dx + 2, dy + 90, TEXT);
        }
        if (q.rewardDeditas > 0) {
            int aw = (int) Math.ceil(EconomyHud.amountWidth(font, q.rewardDeditas) * T);
            amount(g, dx + DET_W - 2 - aw, dy + 90, q.rewardDeditas, GOLD);
        }
        if (!q.rewardItem.isBlank()) {
            ResourceLocation rl = ResourceLocation.tryParse(q.rewardItem);
            if (rl != null) {
                BuiltInRegistries.ITEM.getOptional(rl).ifPresent(item -> {
                    ItemStack stack = new ItemStack(item, q.rewardCount);
                    g.renderItem(stack, dx + 50, dy + 86);
                    g.renderItemDecorations(font, stack, dx + 50, dy + 86);
                    text(g, stack.getHoverName(), dx + 70, dy + 90, GOLD);
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
        String label = switch (q.objectiveType) {
            case "KILL" -> "Derrota: " + target;
            case "OBTAIN" -> "Consigue: " + target;
            default -> "Viaja a: " + q.target;
        };
        return font.plainSubstrByWidth(label, (int) ((DET_W - 50) / T));
    }

    // ---- historia

    private void renderLore(GuiGraphics g, int dx, int dy, int sel) {
        CodexContent.Lore l = visibleLore().get(sel);
        text(g, l.title, dx + 2, dy + 2, GOLD);
        wrapped(g, l.text, dx + 2, dy + 16, DET_W - 4, TEXT, 11);
    }

    // ---- bestiario

    private void renderMob(GuiGraphics g, int dx, int dy, int sel, int lmx, int lmy) {
        CodexContent.Mob m = mobs().get(sel);
        if (!mobKnown(m)) {
            text(g, "???", dx + 2, dy + 2, GOLD);
            wrapped(g, "Criatura no descubierta. Derrota a una para registrarla.", dx + 2, dy + 16, DET_W - 4, DIM, 6);
            return;
        }
        text(g, m.name, dx + 2, dy + 2, GOLD);
        text(g, "Peligro: " + m.danger, dx + 2, dy + 12, dangerColor(m.danger));

        LivingEntity living = entityFor(m);
        if (living != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, dx + 74, dy + 20, dx + 130, dy + 70, 18, 0.0625f, lmx, lmy, living);
        }

        wrapped(g, m.description, dx + 2, dy + 24, 68, TEXT, 6);
        text(g, "Qué hacer:", dx + 2, dy + 74, GOLD);
        wrapped(g, m.tips, dx + 2, dy + 83, DET_W - 4, TEXT, 2);

        if (!m.loot.isEmpty()) {
            text(g, "Botín:", dx + 2, dy + 92, GOLD);
            int ix = dx + 30;
            for (String lootId : m.loot) {
                ResourceLocation rl = ResourceLocation.tryParse(lootId);
                if (rl == null) continue;
                var item = BuiltInRegistries.ITEM.getOptional(rl);
                if (item.isEmpty()) continue;
                if (ix > dx + DET_W - 16) break;
                g.renderItem(new ItemStack(item.get()), ix, dy + 88);
                ix += 17;
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

    // ------------------------------------------------------------------ utilidades de dibujo

    private static int shade(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f));
        int gr = Math.min(255, (int) (((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((argb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, int accent, boolean enabled, boolean hover) {
        int bg = !enabled ? 0xFF16121F : hover ? shade(accent, 0.45f) : 0xFF241E38;
        int border = !enabled ? 0xFF251F35 : hover ? accent : shade(accent, 0.55f);
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        g.fill(x + 1, y + 1, x + 3, y + h - 1, enabled ? accent : 0xFF3B3552);
        String s = font.plainSubstrByWidth(label, (int) ((w - 10) / T));
        text(g, s, x + 5 + (w - 8 - tw(s)) / 2, y + (h - 6) / 2 - 1, enabled ? 0xFFFFFFFF : 0xFF6C6677);
    }

    private void text(GuiGraphics g, String s, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(T, T, 1f);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    private void text(GuiGraphics g, Component c, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(T, T, 1f);
        g.drawString(font, c, 0, 0, color, false);
        g.pose().popPose();
    }

    private int tw(String s) {
        return (int) Math.ceil(font.width(s) * T);
    }

    private void amount(GuiGraphics g, int x, int y, long value, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(T, T, 1f);
        EconomyHud.drawAmount(g, font, 0, 0, value, color);
        g.pose().popPose();
    }

    private void wrapped(GuiGraphics g, String s, int x, int y, int maxW, int color, int maxLines) {
        int line = 0;
        for (var seq : font.split(Component.literal(s), (int) (maxW / T))) {
            g.pose().pushPose();
            g.pose().translate(x, y + line * 8, 0);
            g.pose().scale(T, T, 1f);
            g.drawString(font, seq, 0, 0, color, false);
            g.pose().popPose();
            line++;
            if (line >= maxLines) break;
        }
    }
}
